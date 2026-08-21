package com.benhsoan.port.outbound.interconnection;

public interface PrescriptionInterconnectionGatewayPort {

    PrescriptionInterconnectionGatewayResponse submit(PrescriptionInterconnectionGatewayRequest request);
}
