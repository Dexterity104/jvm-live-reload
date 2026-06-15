package me.seroperson.reload.live.webserver.grpc;

/** Shared defaults applied to every builder on the GRPC proxy data path. */
final class GrpcProxyDefaults {

  /**
   * Inbound message-size cap applied to both the proxy listener and the upstream channel.
   *
   * <p>grpc-java defaults this to 4 MiB and enforces it at the deframing layer, before the
   * pass-through marshaller ever runs. A transparent dev proxy must not impose a cap below what the
   * real client and backend negotiated, so we lift it as high as the API allows ({@code int}
   * bytes). Frame sizes are still bounded by the limits the actual endpoints enforce on their own
   * connections.
   */
  static final int MAX_INBOUND_MESSAGE_SIZE = Integer.MAX_VALUE;

  private GrpcProxyDefaults() {}
}
