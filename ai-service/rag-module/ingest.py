from rag.document_processor import DocumentProcessor
from rag.embeddings import EmbeddingModel
from rag.vector_store import VectorStore
import os


def run_ingestion(data_path: str = "./../data"):
    print("Starting ingestion pipeline...")
    if not os.path.isdir(data_path):
        raise FileNotFoundError(f"data_path not found: {data_path}")

    processor = DocumentProcessor()
    embedding_model = EmbeddingModel()
    vector_store = VectorStore()

    # Reset DB to avoid duplicates
    print("Resetting vector database...")
    vector_store.reset()

    # Process documents
    print("Processing documents...")
    chunks = processor.process(data_path)
    print(f"Chunks created: {len(chunks)}")

    # Prepare data
    texts = [c["text"] for c in chunks]
    metadatas = [
        {
            "source": c["source"],
            "chunk_id": c["chunk_id"]
        }
        for c in chunks
    ]

    # Embed
    print("Generating embeddings...")
    embeddings = embedding_model.embed_documents(texts)

    # Store
    print("Storing in vector database...")
    vector_store.add_documents(texts, embeddings, metadatas)

    print("Ingestion completed successfully ")
    print(f"Documents: {len(set([c['source'] for c in chunks]))}")
    print(f"Chunks stored: {len(chunks)}")


if __name__ == "__main__":
    run_ingestion()