import os
import ollama
from services.logger import log_agent_event


async def call_llm(prompt: str, model: str = "gemma4:31b", require_json: bool = False) -> str:
    try:
        ollama_host = os.getenv("OLLAMA_HOST", "http://127.0.0.1:11434")
        ollama_api_key = os.getenv("OLLAMA_API_KEY", "")
        
        headers = {}
        if ollama_api_key:
            headers["Authorization"] = f"Bearer {ollama_api_key}"
            
        client = ollama.Client(
            host=ollama_host,
            headers=headers if headers else None
        )
             
        kwargs = {}
        if require_json:
            kwargs["format"] = "json"
            
        response = client.chat(
            model=model,
            messages=[{"role": "user", "content": prompt}],
            **kwargs
        )
        
        log_agent_event("LLM_CALL", {
            "provider": "ollama",
            "model": model,
            "status": "success"
        })
        return response["message"]["content"]
    except Exception as e:
        log_agent_event("LLM_ERROR", {
            "provider": "ollama",
            "error": str(e)
        })
        raise e
