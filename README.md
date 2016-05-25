# Crashpad

Finding an apartment in SF is rapidly turning into a chore. Craigslist is spammy, and other not so
spammy services primarily host cooperate listings which are out of my price range. Being a good
little nerd, the natural thing to do is to simply take the noisy data stream (craigslist) and filter
it down to something more usable.

There are a couple of tricks here:

1. `org-mode` output is insanely easy to generate via printf and consume as a human.
2. By keeping a blacklist of listings which have already been seen, re-list spam is managed and the
   crawler will only ever add "new" posts to the org output.
3. A human can then directly work with the org file (edit, delete entries) since duplicate entries
   will never be created and the file is append only.
4. Craigslist hash uniques their images, so it's possible to distinguish dupe posts just on that
   basis even if the title has changed. Spam posters don't have a lot of images to work with it
   seems.
5. `arrdem/crajure` uses some probably excessively aggressive memoization and rate limiting so as
   not to run afoul of Craigslist's rate limits and ip banning. Stock `crajure` doesn't do that.

## About

**Global state**:

- Output `.org` file (emacs org-mode is awesome and easy to `printf`)
- "blacklist" `.edn` file containing listing URLs, titles and preview images for listings which have
  already been considered and added to the `.org` output.

**Pipeline**: 

- Use [`arrdem/crajure`](https://github.com/arrdem/crajure) to interface with craigslist.
- Pull down a whole bunch of listings.
- Filter out listings out of my price range.
- Dedupe by title. (using [`medly.core/distinct-by`](https://github.com/weavejester/medley))
- Dedupe by preview image.
- Use the blacklist to filter out listings by title, preview and URL which have been visited in a
  previous crawl.
- Update the blacklist file with the titles, previews and URLs of the surviving listings.
- Append a new header and elements to the org file.

## License

Copyright Reid 'arrdem' McKenzie 2016, all rights reserved.
