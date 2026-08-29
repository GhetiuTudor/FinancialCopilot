import os

import chromadb
from chromadb.config import Settings
import uuid


_DEFAULT_PERSIST_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "..", "data", "vector_store"
)


class VectorStore:
    def __init__(
        self,
        persist_dir: str = _DEFAULT_PERSIST_DIR,
        collection_name: str = "financial_docs"
    ):
        self.client = chromadb.Client(
            Settings(
                persist_directory=persist_dir,
                is_persistent=True
            )
        )

        self.collection_name = collection_name

        self.collection = self.client.get_or_create_collection(
            name=collection_name
        )

    def reset(self):
        try:
            self.client.delete_collection(self.collection_name)
        except Exception:
            pass

        self.collection = self.client.get_or_create_collection(
            name=self.collection_name
        )

    def add_documents(self, texts, embeddings, metadatas=None):
        ids = [str(uuid.uuid4()) for _ in texts]

        self.collection.add(
            documents=texts,
            embeddings=embeddings,
            metadatas=metadatas or [{} for _ in texts],
            ids=ids
        )

    def query(self, query_embedding, k=3):
        return self.collection.query(
            query_embeddings=[query_embedding],
            n_results=k
        )