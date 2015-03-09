spotify-dns-java
================

This small DNS wrapper library provides some useful pieces of functionality related to SRV lookups.

## Resilience

Sometimes it is useful to default to previously returned, cached values, if a dns lookup should fail
or return an empty result. This behavior is controlled by the retainingDataOnFailures() method in
DnsSrvResolvers.DnsSrvResolverBuilder.

## Watching for Changes

It's often useful to update where you try to connect based on changes in lookup results, and this library
provides functionality that allows you to get notified when things change by implementing this interface (defined in the [ChangeNotifier](src/main/java/com/spotify/dns/ChangeNotifier.java) interface):

```java
  interface Listener<T> {

    /**
     * Signal that set of records changed.
     *
     * @param changeNotification An object containing details about the change
     */
    void onChange(ChangeNotification<T> changeNotification);
  }

  /**
   * A change event containing the current and previous set of records.
   */
  interface ChangeNotification<T> {
    Set<T> current();
    Set<T> previous();
  }
```

Take a look  at the [PollingUsage example](src/test/java/com/spotify/dns/examples/PollingUsage.java) for an example.

## Metrics

If you have a statistics system that can be integrated with using the munin protocol, the method
metered() in DnsSrvResolvers.DnsSrvResolverBuilder enables this in conjunction with the spotify
munin forwarder. Have a look at the
[BasicUsage example](src/test/java/com/spotify/dns/examples/BasicUsage.java) for details on how to
set that up.

## Usage

The entry point to lookups is through an instance of
[DnsSrvResolver](src/main/java/com/spotify/dns/DnsSrvResolver.java) obtained via the
[DnsSrvResolvers](src/main/java/com/spotify/dns/DnsSrvResolvers.java) factory class.

To periodically check a set of records and react to changes, use the
[DnsSrvWatcher](src/main/java/com/spotify/dns/DnsSrvWatcher.java) interface obtained via the
[DnsSrvWatchers](src/main/java/com/spotify/dns/DnsSrvWatchers.java) factory class.

For example code, have a look at
[BasicUsage example](src/test/java/com/spotify/dns/examples/BasicUsage.java) and
[PollingUsage example](src/test/java/com/spotify/dns/examples/PollingUsage.java)

To include the latest released version in your maven project, do:
```xml
  <dependency>
    <groupId>com.spotify</groupId>
    <artifactId>dns</artifactId>
    <version>3.0.0</version>
  </dependency>
```

## License

This software is released under the Apache License 2.0. More information in the file LICENSE
distributed with this project.
