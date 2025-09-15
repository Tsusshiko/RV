/*
 * Copyright (c) 2020 Fraunhofer FOKUS and others. All rights reserved.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contact: mosaic@fokus.fraunhofer.de
 */

package org.eclipse.mosaic.app.tutorial.message;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;

import javax.annotation.Nonnull;

/**
 * Class used as message for inter vehicle communication in contrast
 * to the intra vehicle communication.
 */
public final class InterVehicleMsg extends V2xMessage {
    
    /**
     * Example payload. The sender puts its geo location, and name
     * inside the message and sends it to every possible receiver.
     */
    private final String msgrecebida;
    private final String name;
    private final GeoPoint senderPosition;
    private final EncodedPayload payload;
    private final static long minLen = 128L;


    public InterVehicleMsg(MessageRouting routing, GeoPoint senderPosition, String name, String msgrecebida) {
        super(routing);
        payload = new EncodedPayload(16L, minLen);
        this.senderPosition = senderPosition;
        this.name = name;
        this.msgrecebida = msgrecebida;
    }
    public InterVehicleMsg(MessageRouting routing, GeoPoint senderPosition, String name) {
        super(routing);
        payload = new EncodedPayload(16L, minLen);
        this.senderPosition = senderPosition;
        this.name = name;
        this.msgrecebida = null;
    }
    public String getMsgrecebida() {
        return msgrecebida;
    }
    public GeoPoint getSenderPosition() {
        return senderPosition;
    }
    public String getName() {
        return name;
    }


    @Nonnull
    public EncodedPayload getPayLoad() {
        return payload;
    }

    public EncodedPayload getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("InterVehicleMsg{");
        sb.append("senderPosition=").append(senderPosition);
        sb.append(", name=").append(name);
        sb.append(", dados=").append(msgrecebida);
        sb.append('}');

        return sb.toString();
    }
}
