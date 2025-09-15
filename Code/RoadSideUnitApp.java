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

package org.eclipse.mosaic.app.tutorial;

import org.eclipse.mosaic.app.tutorial.cam.CamSendingApp;
import org.eclipse.mosaic.app.tutorial.message.InterVehicleMsg;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.*;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.GeoArea;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Cam;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Denm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.DenmContent;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Road Side Unit Application used for MOSAIC Tiergarten Tutorial.
 * Sends inter-application messages via broadcast in order to show
 * how to differentiate between intra vehicle and inter vehicle application messages.
 */
public class RoadSideUnitApp extends AbstractApplication<RoadSideUnitOperatingSystem>  implements CommunicationApplication {
    /**
     * Interval at which messages are sent.
     */
    public final List<String> LDMmapa = new ArrayList<>();

    private void sendAdHocBroadcast(V2xMessage msg) {
        getOs().getAdHocModule().sendV2xMessage(msg);
    }
    private void sendAdHocBroadcast(String msgrecebida) {
        final MessageRouting routing =
                getOs().getAdHocModule().createMessageRouting().viaChannel(AdHocChannel.CCH).topoBroadCast();
        final InterVehicleMsg message = new InterVehicleMsg(routing, getOs().getPosition(), getOs().getId(), msgrecebida);
        getOs().getAdHocModule().sendV2xMessage(message);
    }
    private void sendCellModuleBroadcast(V2xMessage msgRecebida) {
        GeoPoint center = getOs().getPosition();
        double radius = 1000.0;
        GeoArea geoArea = new GeoCircle(center, radius);
        MessageRouting routing = getOs().getCellModule().createMessageRouting().geoBroadcastBasedOnUnicast(geoArea);
        if (msgRecebida instanceof Cam cam) {
            try {
                Object deserialized = CamSendingApp.DEFAULT_OBJECT_SERIALIZATION.fromBytes(
                        cam.getUserTaggedValue(), this.getClass().getClassLoader()
                );
                String payload = deserialized.toString();

                InterVehicleMsg cellMsg = new InterVehicleMsg(
                        routing,
                        center,
                        getOs().getId(),
                        payload
                );

                getOs().getCellModule().sendV2xMessage(cellMsg);
                getLog().infoSimTime(this, "Retransmissao da CAM como InterVehicleMsg através de CELL: {}", payload);

            } catch (Exception e) {
                getLog().error("Erro:", e);
            }
        }
    }

    public void sample() {
    }

    @Override
    public void onStartup() {
        getLog().infoSimTime(this, "Initialize application");
        getOs().getAdHocModule().enable(new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(500)
                .create());

        getOs().getCellModule().enable(new CellModuleConfiguration());

    }
    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
        V2xMessage msg = receivedV2xMessage.getMessage();
        getLog().infoSimTime(this, "RSU received a V2X message: {}", receivedV2xMessage.getMessage().getSimpleClassName());

        if (msg instanceof Cam cam) {

            try {
                Object deserializedValue = CamSendingApp.DEFAULT_OBJECT_SERIALIZATION.fromBytes(cam.getUserTaggedValue(), this.getClass().getClassLoader());
                String vehicleId = deserializedValue.toString().split(" ")[2];
                V2xMessage message= receivedV2xMessage.getMessage();

                this.getLog().infoSimTime(this, "CAM message arrived, userTaggedValue: {}", new Object[]{CamSendingApp.DEFAULT_OBJECT_SERIALIZATION.fromBytes(cam.getUserTaggedValue(), this.getClass().getClassLoader())});
                //String msgs = deserializedValue.toString();
                //sendAdHocBroadcast(msgs);
                sendCellModuleBroadcast(message);
                if(!LDMmapa.contains(vehicleId)){
                    LDMmapa.add(vehicleId);
                    this.getLog().infoSimTime(this, "veiculo adicionado a vizinhos {}", new Object[]{vehicleId});
                    this.getLog().infoSimTime(this, "lista de vizinhos atuais: {}", new Object[]{LDMmapa});

                }

            } catch (ClassNotFoundException | IOException var5) {
                this.getLog().error("An error occurred", var5);
            }
        }
        if (msg instanceof Denm denm) {

            V2xMessage message= receivedV2xMessage.getMessage();
            this.getLog().infoSimTime(this, "Denm message arrived, : {}", message);
            MessageRouting adHocRouting = getOs().getAdHocModule()
                    .createMessageRouting()
                    .viaChannel(AdHocChannel.CCH)
                    .topoBroadCast();
            Denm adHocDenm = new Denm(
                    adHocRouting,
                    denm,
                    0L    // Minimal payload length
            );

            sendAdHocBroadcast(adHocDenm);

            this.getLog().infoSimTime(this, "Denm message sent, : {}", message);
        }

    }
    @Override
    public void onShutdown() {
        getLog().infoSimTime(this, "Shutdown application");
    }
    public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgedMessage) {
    }
    public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
    }
    public void onCamBuilding(CamBuilder camBuilder) {
    }

    @Override
    public void processEvent(Event event) throws Exception {
    }
}
