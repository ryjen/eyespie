import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'

const MODEL_URL = 'http://dubnium.tail4d84c.ts.net/models/gemma-3n-E2B-it-int4.task'

serve(async (_req) => {
  try {
    const response = await fetch(MODEL_URL)

    if (!response.ok || !response.body) {
      throw new Error(`Failed to fetch model: ${response.statusText}`)
    }

    const headers = new Headers({
      'Content-Type': 'application/octet-stream',
      'Content-Disposition': `attachment; filename="gemma-3n-E2B-it-int4.task"`,
    })

    // Pass through relevant headers from the original response
    if (response.headers.has('Content-Length')) {
      headers.set('Content-Length', response.headers.get('Content-Length')!)
    }

    return new Response(response.body, {
      status: 200,
      headers: headers,
    })
  } catch (error) {
    console.error(error)
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' },
    })
  }
})
