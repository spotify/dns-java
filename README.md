spotify-dns-java
================

This small DNS wrapper library provides some useful pieces of functionality related to SRV lookups.

## Resilience

Sometimes it is useful to default to previously returned, cached values, if a dns lookup should fail
or return an empty result. This behavior is controlled by the retainingDataOnFailures() method in
DnsSrvResolvers.DnsSrvResolverBuilder.

## Metrics

If you have a statistics system that can be integrated with using the munin protocol, the method
metered() in DnsSrvResolvers.DnsSrvResolverBuilder enables this in conjunction with the spotify
munin forwarder. Have a look at the
[BasicUsage example](src/test/java/com/spotify/dns/examples/BasicUsage.java) for details on how to
set that up.

## Usage

The entry point to lookups is through an instance of
[DnsSrvResolver](src/main/java/com/spotify/dns/DnsSrvResolver.java) obtained via the
[DnsSrvResolvers](src/main/java/com/spotify/dns/DnsSrvResolvers.java) factory class. For example
code, have a look at
[BasicUsage example](src/test/java/com/spotify/dns/examples/BasicUsage.java)

To include the latest released version in your maven project, do:
```
    <dependency>
      <groupId>com.spotify</groupId>
      <artifactId>dns</artifactId>
      <version>2.2.0</version>
    </dependency>
```

## License

This software is released under the Apache License 2.0. More information in the file LICENSE
distributed with this project.