import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function Dashboard() {
  const { user, logout } = useAuth();
  const location = useLocation();

  const isActive = (path: string) => location.pathname === path;

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
          <h1>Dashboard</h1>
        </div>
        
        <div className="dashboard-content">
          <div className="stats-grid">
            <div className="stat-card">
              <h3>System Status</h3>
              <p>All services operational</p>
            </div>
            
            <div className="stat-card">
              <h3>API Endpoints</h3>
              <p>12 active endpoints</p>
            </div>
            
            <div className="stat-card">
              <h3>Documentation</h3>
              <p>Live API docs available</p>
            </div>
          </div>
          
          <div className="quick-actions">
            <h3>Quick Actions</h3>
            <Link to="/api-docs" className="action-button">
              View API Documentation
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}

export default Dashboard;
