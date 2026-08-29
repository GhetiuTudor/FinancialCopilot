import React, { useState, useRef, useEffect } from 'react';
import { createThread, getThread, sendMessage } from '../api/chat';
import type { Message } from '../types/chat';

//cheia pentru persistenta chatului 
const THREAD_STORAGE_KEY = 'copilot.threadId';

//functional component 
export const Chat: React.FC = () => {
    //use state returneaza valoarea + setter
    const [threadId, setThreadId] = useState<string | null>(null);
    const [messages, setMessages] = useState<Message[]>([]);
    const [inputValue, setInputValue] = useState('');
    //ai response lag 
    const [isLoading, setIsLoading] = useState(false);
    //conv creation  lag 
    const [isCreatingThread, setIsCreatingThread] = useState(false);
    const [isInitializing, setIsInitializing] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);
    const initCalledRef = useRef(false);

    // thread din local storage | thread nou 
    useEffect(() => {
        //logica de intitializare ruleaza o data 
        if (initCalledRef.current) return;
        initCalledRef.current = true;

        const initChat = async () => {
            const storedId = localStorage.getItem(THREAD_STORAGE_KEY);

            if (storedId) {
                try {
                    //vine din db prin req in java
                    const thread = await getThread(storedId);
                    setThreadId(storedId);
                    setMessages(thread.messages || []);
                } catch {
                    // 404 - thread sters sau invalid 
                    console.warn('Stored thread invalid, creating new thread');
                    localStorage.removeItem(THREAD_STORAGE_KEY);
                    try {
                        const response = await createThread();
                        setThreadId(response.thread_id);
                        localStorage.setItem(THREAD_STORAGE_KEY, response.thread_id);
                    } catch (err) {
                        console.error('Failed to create thread:', err);
                        setError('Nu s-a putut inițializa conversația.');
                    }
                }
            } else {
                try {
                    const response = await createThread();
                    setThreadId(response.thread_id);
                    localStorage.setItem(THREAD_STORAGE_KEY, response.thread_id);
                } catch (err) {
                    console.error('Failed to create thread:', err);
                    setError('Nu s-a putut inițializa conversația.');
                }
            }

            setIsInitializing(false);
        };

        initChat();
    }, []);

    // Auto-scroll in jos 
    useEffect(() => {messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });}, [messages, isLoading]);

    useEffect(() => {
        if (!isInitializing && !isLoading && inputRef.current) {
            inputRef.current.focus();
        }
    }, [isInitializing, isLoading]);

    // conversatie noua 
    const handleNewConversation = async () => {
        setIsCreatingThread(true);
        setError(null);
        setMessages([]); 

        try {
            const response = await createThread();
            setThreadId(response.thread_id);
            localStorage.setItem(THREAD_STORAGE_KEY, response.thread_id);
            setInputValue('');
        } catch (err) {
            setError( err instanceof Error ? err.message : 'A apărut o eroare. Vă rugăm încercați din nou.');
        } finally {
            setIsCreatingThread(false);
        }
    };

    // Fetch pt mesajele din thread
    const fetchMessages = async (id: string) => {
        try {
            const thread = await getThread(id);
            setMessages(thread.messages || []);
        } catch (err) {
            setError( err instanceof Error ? err.message : 'Nu s-au putut încărca mesajele.');
        }
    };

    // Send a message
    const handleSendMessage = async () => {
        if (!inputValue.trim() || !threadId || isLoading || isInitializing) return;

        const userMessage = inputValue.trim();
        setInputValue('');
        setIsLoading(true);
        setError(null);

        //Message temporar, cu id fals pt ca react are nevoie de ID pentru key 
        const tempUserMessage: Message = {
            id: `temp-${Date.now()}`,
            thread_id: threadId,
            role: 'USER',
            content: userMessage,
            created_at: new Date().toISOString(),
        };
        //append la ultimul state existent 
        setMessages((prev) => [...prev, tempUserMessage]);

        try {
            await sendMessage(threadId, userMessage);
            //mesaje stocate in state 
            await fetchMessages(threadId);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Nu s-a putut trimite mesajul.');
            //scot mesajul bagat optimist si il randez pe cel din response din java 
            setMessages((prev) => prev.filter((m) => m.id !== tempUserMessage.id));
        } finally {
            setIsLoading(false);
        }
    };

    //enter key 
    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    return (
        <div className="chat">
            {/* Header */}
            <div className="chat__header">
                <div>
                    <h2 className="chat__header-title">Copilot</h2>
                    <p className="chat__header-subtitle">
                        {isInitializing ? "Se inițiază..." : "Asistentul tău financiar"}
                    </p>
                </div>
                <button
                    onClick={handleNewConversation}
                    disabled={isCreatingThread || isInitializing}
                    className="btn-secondary"
                    style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}
                >
                    {isCreatingThread ? (
                        <>
                            <span className="spinner" />
                            Se creează...
                        </>
                    ) : (
                        <>
                            <svg style={{ width: '14px', height: '14px' }}
                                fill="none" stroke="currentColor" viewBox="0 0 24 24"
                            >
                                <path strokeLinecap="round" strokeLinejoin="round"
                                    strokeWidth={2} d="M12 4v16m8-8H4" />
                            </svg>
                            Conversație nouă
                        </>
                    )}
                </button>
            </div>

            {/* Messages Area */}
            <div className="chat__messages">
                {(isInitializing) && (
                    <div className="chat__empty-state">
                        <span className="spinner" style={{ marginBottom: '0.5rem' }} />
                        <p className="chat__empty-body">Se pregătește conversația...</p>
                    </div>
                )}

                {!isInitializing && !threadId && !error && messages.length === 0 && (
                    <div className="chat__empty-state">
                        <p className="chat__empty-body">Se conectează la server...</p>
                    </div>
                )}

                {!isInitializing && threadId && messages.length === 0 && !isLoading && (
                    <div className="chat__empty-state">
                        <div className="chat__empty-icon">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                                    d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                            </svg>
                        </div>
                        <h3 className="chat__empty-title">Salut!</h3>
                        <p className="chat__empty-body">
                            Sunt aici să te ajut cu informații financiare. Cu ce putem începe?
                        </p>
                    </div>
                )}

                {messages.map((message) => {
                    const isUser = message.role === 'USER';
                    return (
                        <div
                            key={message.id}
                            className={`chat__msg-row ${isUser ? 'chat__msg-row--user' : 'chat__msg-row--assistant'}`}
                        >
                            <div className={`chat__msg-bubble ${isUser ? 'chat__msg-bubble--user' : 'chat__msg-bubble--assistant'}`}>
                                <p className="chat__msg-text">{message.content}</p>
                            </div>
                        </div>
                    );
                })}

                {isLoading && (
                    <div className="chat__msg-row chat__msg-row--assistant">
                        <div className="chat__loading-bubble">
                            <span className="spinner" />
                        </div>
                    </div>
                )}

                <div ref={messagesEndRef} />
            </div>

            {/* Error Message */}
            {error && (
                <div className="chat__error">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                            d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    {error}
                </div>
            )}

            {/* Input Area */}
            <div className="chat__input-area">
                <div className="chat__input-row">
                    <input
                        ref={inputRef}
                        type="text"
                        value={inputValue}
                        onChange={(e) => setInputValue(e.target.value)}
                        onKeyDown={handleKeyDown}
                        placeholder="Scrie un mesaj…"
                        disabled={!threadId || isLoading || isInitializing}
                        className="chat__input"
                    />
                    <button
                        onClick={handleSendMessage}
                        disabled={!inputValue.trim() || !threadId || isLoading || isInitializing}
                        className="btn-primary"
                        style={{ display: 'flex', alignItems: 'center', gap: '0.375rem' }}
                    >
                        {isLoading ? (
                            <span className="spinner" style={{ width: '14px', height: '14px' }} />
                        ) : (
                            <svg style={{ width: '14px', height: '14px' }}
                                fill="none" stroke="currentColor" viewBox="0 0 24 24"
                            >
                                <path strokeLinecap="round" strokeLinejoin="round"
                                    strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                            </svg>
                        )}
                        <span className="sr-only">Trimite</span>
                    </button>
                </div>
            </div>
        </div>
    );
};
