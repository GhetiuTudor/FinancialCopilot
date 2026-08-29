"""
"""

import os
import re
# pyrefly: ignore [missing-import]
from pypdf import PdfReader


_ARTICLE_RE = re.compile(
    r"^(ART\.|Art\.|ARTICOLUL|Articolul)\s*\.?\s*(\d+)", re.MULTILINE
)
_CHAPTER_RE = re.compile(
    r"^(CAPITOLUL|CAP\.)\s+([IVXLCDM\d]+.*)", re.MULTILINE
)
_SECTION_RE = re.compile(
    r"^(SECȚIUNEA|SEC[ȚT]IUNEA|Secțiunea|Sectiunea)\s+(.*)", re.MULTILINE
)
_ALINEAT_RE = re.compile(r"(?=\(\d+\)\s)")


_BOILERPLATE_PATTERNS = [
    re.compile(r"^\s*pagina\s+\d+\s*(din\s+\d+)?\s*$", re.IGNORECASE),
    re.compile(r"^\s*pag\.\s*\d+\s*$", re.IGNORECASE),
    re.compile(r"^\s*\d+\s*$"),  # standalone page numbers
    re.compile(r"^\s*monitorul oficial", re.IGNORECASE),
    re.compile(r"^\s*publicat în", re.IGNORECASE),
    re.compile(r"^\s*www\.\S+\s*$"),  # lone URLs
]


class DocumentProcessor:
    """
    Loads raw documents and converts them into clean, structure-aware chunks.
    """

    def __init__(self, chunk_size: int = 1200, overlap: int = 150):
        self.chunk_size = chunk_size
        self.overlap = overlap

   

    def load_txt(self, file_path: str) -> str:
        with open(file_path, "r", encoding="utf-8") as f:
            return f.read()

    def load_pdf(self, file_path: str) -> str:
        reader = PdfReader(file_path)
        pages = []
        for page in reader.pages:
            text = page.extract_text()
            if text:
                pages.append(text)
        return "\n\n".join(pages)

    def load_documents(self, folder_path: str) -> list[dict]:
        docs = []
        for filename in sorted(os.listdir(folder_path)):
            file_path = os.path.join(folder_path, filename)
            if not os.path.isfile(file_path):
                continue

            text = None
            if filename.lower().endswith(".txt"):
                text = self.load_txt(file_path)
            elif filename.lower().endswith(".pdf"):
                text = self.load_pdf(file_path)

            if text and text.strip():
                docs.append({"source": filename, "text": text})
        return docs


    def clean_text(self, text: str) -> str:
        """Clean raw text: fix encoding, remove boilerplate, collapse whitespace."""
        text = text.replace("\xa0", " ") #non breaking space 
        text = text.replace("\r\n", "\n")

        # Remove boilerplate lines
        lines = text.split("\n")
        cleaned_lines = []
        for line in lines:
            stripped = line.strip()
            if any(p.match(stripped) for p in _BOILERPLATE_PATTERNS):
                continue
            cleaned_lines.append(line)
        text = "\n".join(cleaned_lines)

        
        while "\n\n\n" in text:
            text = text.replace("\n\n\n", "\n\n")

        return text.strip()


    @staticmethod
    def is_quality_chunk(text: str, min_chars: int = 50, min_words: int = 8) -> bool:
        """Reject chunks that are too short, too few words, or mostly OCR noise."""
        stripped = text.strip()
        if len(stripped) < min_chars:
            return False
        words = stripped.split()
        if len(words) < min_words:
            return False
        # Excessive non-alpha content (# 123 $$ @@ 345 )
        alpha_count = sum(1 for c in stripped if c.isalpha())
        if len(stripped) > 0 and alpha_count / len(stripped) < 0.4:
            return False
        return True


    def _detect_legal_boundaries(self, text: str) -> list[tuple[int, str, str]]:
        """
        Find article/chapter/section boundaries.
        Returns list of (char_offset, label, matched_text).
        """
        boundaries = []
        for pattern, label in [
            (_CHAPTER_RE, "chapter"),
            (_SECTION_RE, "section"),
            (_ARTICLE_RE, "article"),
        ]:
            for match in pattern.finditer(text):
                boundaries.append((match.start(), label, match.group(0).strip()))
        boundaries.sort(key=lambda x: x[0])
        return boundaries

   

    def chunk_legal_text(self, text: str, source: str) -> list[dict]:
        """
        Split text at legal article boundaries, keeping articles whole.
        Falls back to paragraph chunking if no legal structure is detected.
        """
        boundaries = self._detect_legal_boundaries(text)

        if not boundaries:
            return self._chunk_paragraphs(text, source)

        chunks = []
        current_chapter = ""
        current_section = ""

        for i, (start, label, matched) in enumerate(boundaries):
            end = boundaries[i + 1][0] if i + 1 < len(boundaries) else len(text)
            segment = text[start:end].strip()

            if label == "chapter":
                current_chapter = matched
                continue
            elif label == "section":
                current_section = matched
                continue

            #article number
            art_match = re.match(r"(?:ART\.|Art\.|ARTICOLUL|Articolul)\s*\.?\s*(\d+)", segment)
            art_number = art_match.group(1) if art_match else "?"

            if len(segment) <= self.chunk_size:
                chunks.append({
                    "text": segment,
                    "source": source,
                    "article": f"ART. {art_number}",
                    "chapter": current_chapter,
                    "section": current_section,
                    "chunk_type": "article",
                })
            else:
                # spilt long articles at alineat boundaries
                sub_chunks = self._split_article_by_alineat(
                    segment, source, art_number, current_chapter, current_section
                )
                chunks.extend(sub_chunks)

        if not chunks:
            return self._chunk_paragraphs(text, source)

        return chunks

    def _hard_split_chunk(
        self, text: str, source: str, art_number: str,
        chapter: str, section: str,
    ) -> list[dict]:
        """
        Last-resort character-level split for text that exceeds chunk_size
        even after alineat splitting. Splits at sentence boundaries when possible.
        """
        
        sentence_re = re.compile(r"(?<=\.)\s+")
        sentences = sentence_re.split(text)

        chunks = []
        current = ""

        for sentence in sentences:
            candidate = (current + " " + sentence).strip() if current else sentence
            if len(candidate) > self.chunk_size and current.strip():
                chunks.append({
                    "text": current.strip(),
                    "source": source,
                    "article": f"ART. {art_number}",
                    "chapter": chapter,
                    "section": section,
                    "chunk_type": "article_part",
                })
                current = sentence
            else:
                current = candidate

        if current.strip():
            chunks.append({
                "text": current.strip(),
                "source": source,
                "article": f"ART. {art_number}",
                "chapter": chapter,
                "section": section,
                "chunk_type": "article_part",
            })
        return chunks

    def _split_article_by_alineat(
        self, text: str, source: str, art_number: str,
        chapter: str, section: str,
    ) -> list[dict]:
        """
        Split a long article at alineat '(1)', '(2)' boundaries.
        The article header is carried into every sub-chunk as overlap context.
        Falls back to sentence-level splitting if chunks remain oversized.
        """
        parts = _ALINEAT_RE.split(text)
        header = parts[0].strip() if parts else ""

        raw_chunks = []
        current = header

        for part in parts[1:]:
            candidate = current + "\n" + part
            if len(candidate) > self.chunk_size and current.strip():
                raw_chunks.append(current.strip())
                
                current = header + "\n" + part
            else:
                current = candidate

        if current.strip():
            raw_chunks.append(current.strip())

        
        final_chunks = []
        for chunk_text in raw_chunks:
            if len(chunk_text) > self.chunk_size * 1.5:
                final_chunks.extend(
                    self._hard_split_chunk(
                        chunk_text, source, art_number, chapter, section
                    )
                )
            else:
                final_chunks.append({
                    "text": chunk_text,
                    "source": source,
                    "article": f"ART. {art_number}",
                    "chapter": chapter,
                    "section": section,
                    "chunk_type": "article_part",
                })
        return final_chunks


    def _chunk_paragraphs(self, text: str, source: str) -> list[dict]:
        """
        Fallback chunking for non-legal documents.
        Merges small paragraphs, splits large ones with overlap.
        """
        paragraphs = [p.strip() for p in text.split("\n\n") if p.strip()]
        chunks = []
        current_chunk = ""

        for paragraph in paragraphs:
            if len(paragraph) > self.chunk_size:
                if current_chunk:
                    chunks.append(current_chunk.strip())
                    current_chunk = ""
                
                start = 0
                while start < len(paragraph):
                    end = start + self.chunk_size
                    piece = paragraph[start:end].strip()
                    if piece:
                        chunks.append(piece)
                    if end >= len(paragraph):
                        break
                    start += self.chunk_size - self.overlap
                continue

            candidate = (current_chunk + "\n\n" + paragraph).strip() if current_chunk else paragraph
            if len(candidate) <= self.chunk_size:
                current_chunk = candidate
            else:
                if current_chunk:
                    chunks.append(current_chunk.strip())
                current_chunk = paragraph

        if current_chunk:
            chunks.append(current_chunk.strip())


        return [
            {
                "text": c,
                "source": source,
                "article": "",
                "chapter": "",
                "section": "",
                "chunk_type": "paragraph",
            }
            for c in chunks
        ]

    

    def process(self, folder_path: str) -> list[dict]:
        raw_docs = self.load_documents(folder_path)
        processed_chunks = []

        for doc in raw_docs:
            clean = self.clean_text(doc["text"])
            chunks = self.chunk_legal_text(clean, doc["source"])

            for i, chunk in enumerate(chunks):
                if not self.is_quality_chunk(chunk["text"]):
                    continue

                chunk["chunk_id"] = i
                processed_chunks.append(chunk)

        return processed_chunks