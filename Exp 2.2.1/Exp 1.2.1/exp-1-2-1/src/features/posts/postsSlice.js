import { createSlice, createEntityAdapter, createAsyncThunk } from '@reduxjs/toolkit';

const postsAdapter = createEntityAdapter({
  selectId: (post) => post.id,
  sortComparer: (a, b) => b.id.localeCompare(a.id),
});

export const fetchPosts = createAsyncThunk('posts/fetchPosts', async () => {
  await new Promise((resolve) => setTimeout(resolve, 800));
  return [
    { id: 'p101', title: 'Getting Started with RTK', platformId: 'plt1', content: 'Redux Toolkit makes global state easy.' },
    { id: 'p102', title: 'Why State Normalization Matters', platformId: 'plt2', content: 'Normalized state acts like a mini relational DB in frontend.' },
  ];
});

const postsSlice = createSlice({
  name: 'posts',
  initialState: postsAdapter.getInitialState({
    status: 'idle',
    error: null,
  }),
  reducers: {
    postAdded: (state, action) => {
      postsAdapter.addOne(state, action.payload);
    },
    postDeleted: (state, action) => {
      postsAdapter.removeOne(state, action.payload);
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchPosts.pending, (state) => {
        state.status = 'loading';
      })
      .addCase(fetchPosts.fulfilled, (state, action) => {
        state.status = 'succeeded';
        postsAdapter.upsertMany(state, action.payload);
      })
      .addCase(fetchPosts.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.error.message;
      });
  },
});

export const { postAdded, postDeleted } = postsSlice.actions;

export const { selectAll: selectAllPosts } = postsAdapter.getSelectors((state) => state.posts);

export default postsSlice.reducer;
