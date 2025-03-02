# usrv

A small, simple, performant server for static sites

# Why?

- nginx can be hard to configure
- A simple server that I've written can be tweaked easily for project demands
- It's fun!

## nginx config

```
server {
    listen 5173;
    server_name localhost;

    root /usr/share/nginx/html;
    index index.html;

    # Enable compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 30d;
        add_header Cache-Control "public, no-transform";
    }

    # SPA configuration - redirect all requests to index.html
    location / {
        try_files $uri $uri/ /index.html$is_args$args;

        # Security headers
        add_header X-Frame-Options "SAMEORIGIN";
        add_header X-XSS-Protection "1; mode=block";
        add_header X-Content-Type-Options "nosniff";
    }

    # Don't cache index.html
    location = /index.html {
        expires -1;
        add_header Cache-Control "no-store, no-cache, must-revalidate";
    }

    # Handle 404
    error_page 404 /index.html;
}
```

## Goal:

*To create a simple static server for small projects*

Support for the basic HTTP methods

- GET
- HEAD

Easy to configure

- Use command-line arguments
- YML config with simple semantics

Sane defaults for its purpose

Simple caching

Single page application support

## Current status

*To create a simple static server for small projects*

Support for the basic HTTP methods

- GET
- ~~HEAD~~

Easy to configure

- ~~Use command-line arguments~~
- ~~YML config with simple semantics~~

Sane defaults for its purpose

Simple caching

Single page application support

## What does a HTTP request look like?

```
GET / HTTP/1.1
Host: localhost
User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:134.0) Gecko/20100101 Firefox/134.0
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
Accept-Language: en-US,en;q=0.5
Accept-Encoding: gzip, deflate, br, zstd
Connection: keep-alive
```

## Response

```
HTTP/1.1 200 OK
Server: usrv
Content-Type: text/html
Date: Fri, 28 Feb 2025 11:32:26 Z
Connection: close

<html>
<h1>Hello world!</h1>
</html>
```

# Demo time!