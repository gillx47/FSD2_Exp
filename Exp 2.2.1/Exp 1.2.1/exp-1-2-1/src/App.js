import React from 'react';
import { PostsList } from './features/posts/PostsList';
import { PlatformsList } from './features/platforms/PlatformsList';
import './App.css';

function App() {
  return (
    <div className="app-container">
      <header>
        <h1>Experiment 1.2.1: Centralized Redux State</h1>
      </header>
      <main className="content-grid">
        <PostsList />
        <PlatformsList />
      </main>
    </div>
  );
}

export default App;
