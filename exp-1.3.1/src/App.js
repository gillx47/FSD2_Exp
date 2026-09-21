import React, { useState, useEffect } from 'react';

function App() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [token, setToken] = useState(localStorage.getItem('jwt_token') || '');
  const [protectedData, setProtectedData] = useState('');

  // Simulated login handler
  const handleLogin = (e) => {
    e.preventDefault();
    if (username === 'admin' && password === 'password123') {
      // Simulated JWT payload and token structure
      const simulatedToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoiYWRtaW4iLCJyb2xlIjoiYWRtaW4ifQ.simulate_signature_hash';
      localStorage.setItem('jwt_token', simulatedToken);
      setToken(simulatedToken);
      alert('Login successful! JWT stored securely in localStorage.');
    } else {
      alert('Invalid credentials! Use: admin / password123');
    }
  };

  // Simulated logout handler
  const handleLogout = () => {
    localStorage.removeItem('jwt_token');
    setToken('');
    setProtectedData('');
  };

  // Simulate fetching protected data using the stored token
  const fetchProtectedResource = () => {
    if (token) {
      setProtectedData('🎉 Success! Access granted to protected backend resource using the JWT token.');
    } else {
      setProtectedData('❌ Access Denied: No valid token found.');
    }
  };

  return (
    <div style={{ padding: '40px', fontFamily: 'Arial, sans-serif', maxWidth: '600px', margin: 'auto' }}>
      <h2>Experiment 1.3.1: JWT Authentication System</h2>
      <p>Demonstrating stateless authentication, token-based session management, and secure storage.</p>
      <hr style={{ margin: '20px 0' }} />

      {!token ? (
        <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <h3>User Login</h3>
          <input 
            type="text" 
            placeholder="Username (e.g., admin)" 
            value={username} 
            onChange={(e) => setUsername(e.target.value)} 
            style={{ padding: '8px', fontSize: '14px' }}
            required 
          />
          <input 
            type="password" 
            placeholder="Password (e.g., password123)" 
            value={password} 
            onChange={(e) => setPassword(e.target.value)} 
            style={{ padding: '8px', fontSize: '14px' }}
            required 
          />
          <button type="submit" style={{ padding: '10px', background: '#007bff', color: '#fff', border: 'none', cursor: 'pointer' }}>
            Login & Generate Token
          </button>
        </form>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
          <div style={{ background: '#e2f0d9', padding: '15px', borderRadius: '5px' }}>
            <h3 style={{ margin: '0 0 10px 0', color: '#385723' }}>Session Active (Stateless)</h3>
            <p style={{ wordBreak: 'break-all', background: '#ffffff', padding: '10px', border: '1px solid #c8e1b7' }}>
              <strong>Stored JWT:</strong> {token}
            </p>
          </div>

          <button onClick={fetchProtectedResource} style={{ padding: '10px', background: '#28a745', color: '#fff', border: 'none', cursor: 'pointer' }}>
            Access Protected Resource
          </button>

          {protectedData && (
            <div style={{ background: '#f8f9fa', padding: '10px', borderLeft: '4px solid #007bff' }}>
              <p style={{ margin: 0 }}>{protectedData}</p>
            </div>
          )}

          <button onClick={handleLogout} style={{ padding: '10px', background: '#dc3545', color: '#fff', border: 'none', cursor: 'pointer' }}>
            Logout & Clear Token
          </button>
        </div>
      )}
    </div>
  );
}

export default App;
