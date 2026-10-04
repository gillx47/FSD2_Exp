import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { fetchPosts, selectAllPosts, postAdded, postDeleted } from './postsSlice';
import { selectAllPlatforms } from '../platforms/platformsSlice';

export function PostsList() {
  const dispatch = useDispatch();
  const posts = useSelector(selectAllPosts);
  const platforms = useSelector(selectAllPlatforms);
  const status = useSelector((state) => state.posts.status);

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [platformId, setPlatformId] = useState('plt1');

  useEffect(() => {
    if (status === 'idle') {
      dispatch(fetchPosts());
    }
  }, [status, dispatch]);

  const handleCreatePost = (e) => {
    e.preventDefault();
    if (!title || !content) return;

    dispatch(
      postAdded({
        id: \p_\\,
        title,
        content,
        platformId,
      })
    );

    setTitle('');
    setContent('');
  };

  return (
    <div className="card">
      <h2>Posts Management</h2>

      <form onSubmit={handleCreatePost} className="form-group">
        <input
          type="text"
          placeholder="Post Title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
        />
        <textarea
          placeholder="Post Content"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          required
        />
        <select value={platformId} onChange={(e) => setPlatformId(e.target.value)}>
          {platforms.map((plt) => (
            <option key={plt.id} value={plt.id}>
              {plt.name}
            </option>
          ))}
        </select>
        <button type="submit">Add Post</button>
      </form>

      {status === 'loading' && <p>Loading posts from mock API...</p>}

      <div className="post-list">
        {posts.map((post) => {
          const platform = platforms.find((p) => p.id === post.platformId);
          return (
            <div key={post.id} className="post-item">
              <h3>{post.title}</h3>
              <p>{post.content}</p>
              <small>Platform: {platform ? platform.name : 'Unknown'}</small>
              <br />
              <button
                className="btn-danger"
                onClick={() => dispatch(postDeleted(post.id))}
              >
                Delete
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}
