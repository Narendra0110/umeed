import { getAuth } from '@react-native-firebase/auth';

import { ApiError, apiFetch, errorMessage, fileUrl } from '@/lib/api';

jest.mock('@react-native-firebase/auth', () => ({ getAuth: jest.fn() }));

const mockedGetAuth = getAuth as jest.Mock;
const fetchMock = jest.fn();

function respond(status: number, body?: unknown) {
  fetchMock.mockResolvedValueOnce({
    status,
    ok: status >= 200 && status < 300,
    text: async () => (body === undefined ? '' : JSON.stringify(body)),
  });
}

beforeEach(() => {
  fetchMock.mockReset();
  globalThis.fetch = fetchMock as unknown as typeof fetch;
  mockedGetAuth.mockReturnValue({ currentUser: { getIdToken: jest.fn().mockResolvedValue('test-token') } });
});

describe('apiFetch', () => {
  it('sends the Firebase token and returns the JSON body', async () => {
    respond(200, [{ id: '1', slug: 'clothes', name: 'Clothes' }]);

    const result = await apiFetch('/api/v1/categories');

    expect(result).toEqual([{ id: '1', slug: 'clothes', name: 'Clothes' }]);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toMatch(/\/api\/v1\/categories$/);
    expect(init.method).toBe('GET');
    expect(init.headers.Authorization).toBe('Bearer test-token');
    expect(init.headers['Content-Type']).toBeUndefined();
    expect(init.body).toBeUndefined();
  });

  it('sends an object body as JSON', async () => {
    respond(201, { id: 'c1' });

    await apiFetch('/api/v1/donations/d1/claims', { method: 'POST', body: { message: 'Hello' } });

    const [, init] = fetchMock.mock.calls[0];
    expect(init.method).toBe('POST');
    expect(init.headers['Content-Type']).toBe('application/json');
    expect(init.body).toBe('{"message":"Hello"}');
  });

  it('sends a FormData body as multipart without setting Content-Type itself', async () => {
    respond(201, { id: 'd1' });
    const form = new FormData();
    form.append('title', 'Bag');

    await apiFetch('/api/v1/donations', { method: 'POST', body: form });

    const [, init] = fetchMock.mock.calls[0];
    expect(init.body).toBe(form);
    expect(init.headers['Content-Type']).toBeUndefined();
  });

  it('returns nothing for 204 No Content', async () => {
    respond(204);
    await expect(apiFetch('/api/v1/donations/d1', { method: 'DELETE' })).resolves.toBeUndefined();
  });

  it('turns the backend error shape into an ApiError with field errors', async () => {
    respond(400, {
      status: 400,
      message: 'Validation failed',
      fieldErrors: [{ field: 'title', message: 'Title is required' }],
    });

    const error = (await apiFetch('/api/v1/donations', { method: 'POST', body: {} }).catch((e: unknown) => e)) as ApiError;

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(400);
    expect(error.message).toBe('Validation failed');
    expect(error.fieldErrors).toEqual([{ field: 'title', message: 'Title is required' }]);
    expect(errorMessage(error)).toBe('Title is required');
  });

  it('uses a friendly message when the error body is not JSON', async () => {
    fetchMock.mockResolvedValueOnce({ status: 500, ok: false, text: async () => '<html>' });
    const error = (await apiFetch('/api/v1/donations').catch((e: unknown) => e)) as ApiError;
    expect(error.status).toBe(500);
    expect(errorMessage(error)).toBe('Something went wrong. Please try again.');
  });

  it('reports a network failure as status 0 and logs the cause in development', async () => {
    // The real cause is logged in development builds; keep it out of the test output.
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});
    const cause = new TypeError('Network request failed');
    fetchMock.mockRejectedValueOnce(cause);
    const error = (await apiFetch('/api/v1/donations').catch((e: unknown) => e)) as ApiError;
    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(0);
    expect(warn).toHaveBeenCalledWith(expect.stringContaining('GET'), cause);
    warn.mockRestore();
  });

  it('does not call the server when nobody is logged in', async () => {
    mockedGetAuth.mockReturnValue({ currentUser: null });
    const error = (await apiFetch('/api/v1/users/me').catch((e: unknown) => e)) as ApiError;
    expect(error.status).toBe(401);
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

describe('fileUrl', () => {
  it('puts the server address in front of a relative photo path', () => {
    expect(fileUrl('/api/v1/files/donations/a.jpg')).toMatch(/^https?:\/\/.+\/api\/v1\/files\/donations\/a\.jpg$/);
  });

  it('leaves a full address unchanged', () => {
    expect(fileUrl('https://cdn.example.com/a.jpg')).toBe('https://cdn.example.com/a.jpg');
  });
});
