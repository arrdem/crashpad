# Crashpad

Finding an apartment in SF is rapidly turning into a chore.
Craigslist is spammy, and other not so spammy services primarily host corporate listings which are out of my price range.
Being a good little nerd, the natural thing to do is to simply take the noisy data stream (craigslist) and filter it down to something more usable.

## About

This is single entry point app which I run by hand at a REPL or via a cronjob.

**Global state**:

- Output `.org` file (emacs org-mode is awesome and easy to `printf`)
- "blacklist" `.edn` file containing listing URLs, titles and preview images for listings which have
  already been considered and added to the `.org` output.

**Pipeline**: 

- Use [`arrdem/crajure`](https://github.com/arrdem/crajure) to interface with craigslist.
- Pull down a whole bunch of listings.
- Use the blacklist to filter out listings by title, preview and URL which have been visited in a
  previous crawl.
- Update the blacklist file with the titles, previews and URLs of the surviving listings.
- Append a new header and elements to the org file.

## License

Copyright Reid 'arrdem' McKenzie 2016, all rights reserved.
