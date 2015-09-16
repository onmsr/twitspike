package jp.co.dwango.twitspike.filters

import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.headers.SecurityHeadersFilter
import play.filters.cors.CORSFilter

class APISecurityFilters @Inject()(securityHeadersFilter: SecurityHeadersFilter, corsFilter: CORSFilter) extends HttpFilters {
  def filters = Seq(securityHeadersFilter, corsFilter)
}
