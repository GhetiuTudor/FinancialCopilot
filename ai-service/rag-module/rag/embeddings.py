from sentence_transformers import SentenceTransformer


class EmbeddingModel:
    """
    Embedding wrapper for multilingual semantic search.
    """

    def __init__(self, model_name: str = "intfloat/multilingual-e5-large"):  #1024 dimensiuni 
        self.model_name = model_name
        self.model = SentenceTransformer(model_name)

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        texts = [f"passage: {t}" for t in texts]
        return self.model.encode(texts, normalize_embeddings=True).tolist()

    def embed_query(self, query: str) -> list[float]:
        return self.model.encode(f"query: {query}", normalize_embeddings=True).tolist()