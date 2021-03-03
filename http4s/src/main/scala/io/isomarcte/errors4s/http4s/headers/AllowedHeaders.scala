package io.isomarcte.errors4s.http4s.headers

import cats.data._
import cats.syntax.all._
import org.http4s.util.CaseInsensitiveString

object AllowedHeaders {

  lazy val defaultAllowHeaders: NonEmptySet[CaseInsensitiveString] =
    (
      defaultAllowAuthenticationHeaders ++ defaultAllowCachingHeaders ++ defaultAllowClientHintsHeaders ++
        defaultAllowConditionalsHeaders ++ defaultAllowConnectionManagementHeaders ++
        defaultAllowContentNegotiationHeaders ++ defaultAllowControlsHeaders ++ defaultAllowCORSHeaders ++
        defaultAllowDoNotTrackHeaders ++ defaultAllowDownloadHeaders ++ defaultAllowMessageBodyInformationHeaders ++
        defaultAllowProxiesHeaders ++ defaultAllowRedirectsHeaders ++ defaultAllowRequestContextHeaders ++
        defaultAllowResponseContextHeaders ++ defaultAllowRangeRequestsHeaders ++ defaultAllowSecurityHeaders ++
        defaultAllowHPKPHeaders ++ defaultAllowFetchMetadataRequestHeaders ++ defaultAllowTransferCodingHeaders ++
        defaultAllowOtherHeaders
    ).map(value => CaseInsensitiveString(value))

  private lazy val defaultAllowAuthenticationHeaders: NonEmptySet[String] = NonEmptySet
    .of("WWW-Authenticate", "Proxy-Authenticate")

  private lazy val defaultAllowCachingHeaders: NonEmptySet[String] = NonEmptySet
    .of("Age", "Cache-Control", "Clear-Site-Data", "Expires", "Pragma", "Warning")

  private lazy val defaultAllowClientHintsHeaders: NonEmptySet[String] = NonEmptySet
    .of("Accept-CH", "Accept-CH-Liftetime", "Early-Data", "Device-Memory", "Save-Data", "Viewport-Width", "Width")

  private lazy val defaultAllowConditionalsHeaders: NonEmptySet[String] = NonEmptySet
    .of("Last-Modified", "ETag", "If-Match", "If-None-Match", "If-Modified-Since", "If-Unmodified-Since", "Vary")

  private lazy val defaultAllowConnectionManagementHeaders: NonEmptySet[String] = NonEmptySet
    .of("Connection", "Keep-Alive")

  private lazy val defaultAllowContentNegotiationHeaders: NonEmptySet[String] = NonEmptySet
    .of("Accept", "Accept-Charset", "Accept-Encoding", "Accept-Language")

  private lazy val defaultAllowControlsHeaders: NonEmptySet[String] = NonEmptySet.of("Expect", "Max-Forwards")

  private lazy val defaultAllowCORSHeaders: NonEmptySet[String] = NonEmptySet.of(
    "Access-Control-Allow-Origin",
    "Access-Control-Allow-Credentials",
    "Access-Control-Allow-Headers",
    "Access-Control-Expose-Methods",
    "Access-Control-Max-Age",
    "Access-Control-Request-Headers",
    "Access-Control-Request-Method",
    "Origin",
    "Timing-Allow-Origin"
  )

  private lazy val defaultAllowDoNotTrackHeaders: NonEmptySet[String] = NonEmptySet.of("DNT", "Tk")

  private lazy val defaultAllowDownloadHeaders: NonEmptySet[String] = NonEmptySet.of("Content-Disposition")

  private lazy val defaultAllowMessageBodyInformationHeaders: NonEmptySet[String] = NonEmptySet
    .of("Content-Length", "Content-Type", "Content-Encoding", "Content-Language", "Content-Location")

  private lazy val defaultAllowProxiesHeaders: NonEmptySet[String] = NonEmptySet
    .of("Forwarded", "X-Forwarded-For", "X-Forwarded-Host", "X-Forwarded-Proto", "Via")

  private lazy val defaultAllowRedirectsHeaders: NonEmptySet[String] = NonEmptySet.of("Location")

  private lazy val defaultAllowRequestContextHeaders: NonEmptySet[String] = NonEmptySet
    .of("From", "Host", "Referer", "Referer-Policy", "User-Agent")

  private lazy val defaultAllowResponseContextHeaders: NonEmptySet[String] = NonEmptySet.of("Allow", "Server")

  private lazy val defaultAllowRangeRequestsHeaders: NonEmptySet[String] = NonEmptySet
    .of("Accept-Ranges", "Range", "If-Range", "Content-Range")

  private lazy val defaultAllowSecurityHeaders: NonEmptySet[String] = NonEmptySet.of(
    "Cross-Origin-Embedder-Policy",
    "Cross-Origin-Opener-Policy",
    "Cross-Origin-Resource-Policy",
    "Content-Security-Policy",
    "Content-Security-Policy-Report-Only",
    "Expect-CT",
    "Feature-Policy",
    "Strict-Transport-Security",
    "X-Content-Type-Options",
    "X-Download-Options",
    "X-Frame-Options",
    "X-Permitted-Cross-Domain-Policies",
    "X-Powered-By",
    "X-XSS-Protection"
  )

  private lazy val defaultAllowHPKPHeaders: NonEmptySet[String] = NonEmptySet
    .of("Public-Key-Pins", "Public-Key-Pins-Report-Only")

  private lazy val defaultAllowFetchMetadataRequestHeaders: NonEmptySet[String] = NonEmptySet
    .of("Sec-Fetch-Site", "Sec-Fetch-Mode", "Sec-Fetch-User", "Sec-Fetch-Dest")

  private lazy val defaultAllowTransferCodingHeaders: NonEmptySet[String] = NonEmptySet
    .of("Transfer-Encoding", "TE", "Trailer")

  private lazy val defaultAllowOtherHeaders: NonEmptySet[String] = NonEmptySet.of(
    "Alt-Svc",
    "Date",
    "Large-Allocation",
    "Link",
    "Retry-After",
    "Server-Timing",
    "SourceMap",
    "X-SourceMap",
    "Upgrade",
    "X-DNS-Prefetch-Control"
  )
}
