package com.oscar.smartcontrol.protocol.pairing;

interface SecretProvider {

    void requestSecret(PairingSession pairingSession);

}
