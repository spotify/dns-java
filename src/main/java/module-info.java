module com.spotify.dns {

  requires org.dnsjava;
  requires com.google.common;
  requires org.slf4j;

  exports com.spotify.dns;
  exports com.spotify.dns.statistics;
}
