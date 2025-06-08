import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface ApiEndpoint {
  method: string;
  path: string;
  description: string;
  parameters?: any[];
  responses?: any;
  example?: any;
}

interface MarkdownContent {
  filename: string;
  markdown: string;
  html: string;
  lastModified: number;
}

function ApiDocumentation() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [endpoints, setEndpoints] = useState<ApiEndpoint[]>([]);
  const [kbContent, setKbContent] = useState<MarkdownContent | null>(null);
  const [activeTab, setActiveTab] = useState<'reference' | 'live'>('reference');
  const [loading, setLoading] = useState(true);

  const isActive = (path: string) => location.pathname === path;
  useEffect(() => {
    fetchApiDocumentation();
    fetchKbDocumentation();
  }, []);
  const fetchApiDocumentation = async () => {
    try {
      const response = await fetch('/api/docs/endpoints', {
        credentials: 'include' // Include session cookies
      });
      if (response.ok) {
        const data = await response.json();
        setEndpoints(data);
      }
    } catch (error) {
      console.error('Failed to fetch API documentation:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchKbDocumentation = async () => {
    try {
      const response = await fetch('/api/docs/markdown/API_ENDPOINTS_REFERENCE', {
        credentials: 'include' // Include session cookies
      });
      if (response.ok) {
        const data = await response.json();
        setKbContent(data);
      }
    } catch (error) {
      console.error('Failed to fetch KB documentation:', error);
    }
  };
  const testEndpoint = async (endpoint: ApiEndpoint) => {
    try {
      const response = await fetch(endpoint.path, {
        method: endpoint.method,
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include' // Use session-based auth instead of Bearer token
      });
      
      const result = await response.json();
      console.log('Test result:', result);
      alert(`Test completed. Check console for details.`);
    } catch (error) {
      console.error('Test failed:', error);
      alert('Test failed. Check console for details.');
    }
  };

  if (loading) {
    return <div>Loading API documentation...</div>;
  }

  return (
    <div className="dashboard">
      <aside className="sidebar">
        <h3>RecapMap Admin</h3>
        <nav>
          <ul>
            <li>
              <Link 
                to="/dashboard" 
                className={isActive('/dashboard') ? 'active' : ''}
              >
                Dashboard
              </Link>
            </li>
            <li>
              <Link 
                to="/api-docs" 
                className={isActive('/api-docs') ? 'active' : ''}
              >
                API Documentation
              </Link>
            </li>
          </ul>
        </nav>
      </aside>
      
      <main className="main-content">
        <button className="logout-button" onClick={logout}>
          Logout ({user})
        </button>
          <div className="content-header">
          <h1>API Documentation</h1>
          <p>Comprehensive documentation for all RecapMap Core API endpoints</p>
        </div>

        <div className="api-tabs">
          <button 
            className={`tab-button ${activeTab === 'reference' ? 'active' : ''}`}
            onClick={() => setActiveTab('reference')}
          >
            📚 Complete Reference
          </button>
          <button 
            className={`tab-button ${activeTab === 'live' ? 'active' : ''}`}
            onClick={() => setActiveTab('live')}
          >
            🔄 Live Endpoints
          </button>
        </div>

        {activeTab === 'reference' && kbContent && (
          <div className="kb-documentation">
            <div 
              className="markdown-content"
              dangerouslySetInnerHTML={{ __html: kbContent.html }}
            />
          </div>
        )}

        {activeTab === 'live' && (
          <div className="api-docs">
            {endpoints.map((endpoint, index) => (
              <div key={index} className="endpoint-section">
                <div className="endpoint-header">
                  <span className={`endpoint-method method-${endpoint.method.toLowerCase()}`}>
                    {endpoint.method}
                  </span>
                  <span className="endpoint-path">{endpoint.path}</span>
                </div>
                
                <div className="endpoint-content">
                  <div className="endpoint-description">
                    {endpoint.description}
                  </div>
                  
                  {endpoint.parameters && endpoint.parameters.length > 0 && (
                    <div>
                      <h4>Parameters</h4>
                      <div className="code-block">
                        <pre>{JSON.stringify(endpoint.parameters, null, 2)}</pre>
                      </div>
                    </div>
                  )}
                  
                  {endpoint.example && (
                    <div>
                      <h4>Example Request</h4>
                      <div className="code-block">
                        <pre>{JSON.stringify(endpoint.example, null, 2)}</pre>
                      </div>
                    </div>
                  )}
                  
                  {endpoint.responses && (
                    <div>
                      <h4>Response Example</h4>
                      <div className="response-example">
                        <pre>{JSON.stringify(endpoint.responses, null, 2)}</pre>
                      </div>
                    </div>
                  )}
                  
                  <div className="live-test-section">
                    <h4>Live Test</h4>
                    <p>Test this endpoint directly from the documentation</p>
                    <button 
                      className="test-button"
                      onClick={() => testEndpoint(endpoint)}
                    >
                      Test Endpoint
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}

export default ApiDocumentation;
