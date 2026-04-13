const { Pact } = require('@pact-foundation/pact');
const { eachLike, like } = require('@pact-foundation/pact').Matchers;
const { PostServiceClient } = require('./post-service-client');

describe('Post Service Contract', () => {
    let provider;
    let postServiceClient;

    beforeAll(() => {
        provider = new Pact({
            consumer: 'feed-service',
            provider: 'post-service',
            port: 1234,
            log: './pact-logs/post-service.log',
            dir: './pacts',
            logLevel: 'INFO',
        });

        return provider.setup().then(() => {
            postServiceClient = new PostServiceClient('http://localhost:1234');
        });
    });

    afterAll(() => {
        return provider.finalize();
    });

    describe('GET /api/v1/posts', () => {
        it('returns posts list', () => {
            const expectedPosts = eachLike({
                id: like('post-123'),
                caption: like('Test post'),
                userId: like('user-456'),
                postType: like('POST'),
            });

            return provider
                .addInteraction({
                    state: 'posts exist',
                    uponReceiving: 'a request for posts',
                    withRequest: {
                        method: 'GET',
                        path: '/api/v1/posts',
                        query: { page: '1', limit: '10' },
                    },
                    willRespondWith: {
                        status: 200,
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: {
                            success: true,
                            data: {
                                data: expectedPosts,
                                total: like(100),
                                page: like(1),
                                limit: like(10),
                            },
                        },
                    },
                })
                .then(() => postServiceClient.getPosts(1, 10))
                .then((posts) => {
                    expect(posts).toBeDefined();
                });
        });
    });

    describe('POST /api/v1/posts', () => {
        it('creates a new post', () => {
            return provider
                .addInteraction({
                    state: 'user exists',
                    uponReceiving: 'a request to create a post',
                    withRequest: {
                        method: 'POST',
                        path: '/api/v1/posts',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: {
                            caption: like('New post'),
                            authorId: like('user-123'),
                            postType: like('POST'),
                            mood: like('HAPPY'),
                        },
                    },
                    willRespondWith: {
                        status: 201,
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: {
                            success: true,
                            data: {
                                id: like('post-456'),
                                caption: like('New post'),
                                userId: like('user-123'),
                            },
                        },
                    },
                })
                .then(() =>
                    postServiceClient.createPost({
                        caption: 'New post',
                        authorId: 'user-123',
                        postType: 'POST',
                        mood: 'HAPPY',
                    })
                )
                .then((post) => {
                    expect(post).toBeDefined();
                    expect(post.id).toBeDefined();
                });
        });
    });
});

