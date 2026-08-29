#java calls this at ai/v1/analyze
#base image for container, debian python, slim - 150mb of python packages 
#faster build
FROM python:3.12-slim

#default workig folder 
WORKDIR /app

# update the list og av soft packages, install curl, no extra dependencies, remove package lists 
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

#install requirements but dont keep the packages, smaller image, faster build 
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt   

#copy the rest of python files 
COPY . .

#service available at port 8000
EXPOSE 8000

# LangGraph agent entrypoint (replaces legacy main:app)
CMD ["uvicorn", "agent.api:app", "--host", "0.0.0.0", "--port", "8000"]
