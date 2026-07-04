import fs from 'node:fs';
import { chromium } from 'playwright-core';

const DEFAULT_LIMIT = 5;

function resolveExecutablePath() {
  const candidates = [
    process.env.PLAYWRIGHT_BROWSER_PATH,
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe'
  ].filter(Boolean);

  return candidates.find(candidate => fs.existsSync(candidate)) || '';
}

function buildPositionLink(postId) {
  if (!postId) {
    return 'https://jobs.bytedance.com/experienced/position';
  }
  return `https://jobs.bytedance.com/experienced/position/${postId}/detail`;
}

function normalizeText(value) {
  return typeof value === 'string' ? value.replace(/\s+/g, ' ').trim() : '';
}

function extractPosts(payload, limit) {
  const jobPosts = payload?.data?.job_post_list;
  if (!Array.isArray(jobPosts)) {
    return [];
  }

  return jobPosts.slice(0, limit).map(post => ({
    id: normalizeText(post?.id),
    title: normalizeText(post?.title),
    link: buildPositionLink(post?.id),
    city: normalizeText(post?.city_info?.name),
    recruitType: normalizeText(post?.recruit_type?.name),
    category: normalizeText(post?.job_category?.name),
    code: normalizeText(post?.code),
    description: normalizeText(post?.description),
    requirement: normalizeText(post?.requirement)
  }));
}

async function main() {
  const keyword = normalizeText(process.argv[2] || '');
  const limit = Number.parseInt(process.argv[3] || '', 10) || DEFAULT_LIMIT;
  const executablePath = resolveExecutablePath();

  if (!keyword || !executablePath) {
    process.stdout.write('[]');
    return;
  }

  const browser = await chromium.launch({
    headless: true,
    executablePath
  });

  try {
    const page = await browser.newPage();
    const targetUrl = `https://jobs.bytedance.com/experienced/position?keywords=${encodeURIComponent(keyword)}&current=1&limit=${limit}`;
    const responsePromise = page.waitForResponse(
      response => response.url().includes('/api/v1/search/job/posts') && response.status() === 200,
      { timeout: 25000 }
    );

    await page.goto(targetUrl, {
      waitUntil: 'domcontentloaded',
      timeout: 45000
    });

    const response = await responsePromise;
    const payload = await response.json();
    process.stdout.write(JSON.stringify(extractPosts(payload, limit)));
  } finally {
    await browser.close();
  }
}

main().catch(() => {
  process.stdout.write('[]');
  process.exit(0);
});
