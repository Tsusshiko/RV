package org.eclipse.mosaic.app.tutorial.rsu;

import org.eclipse.mosaic.app.tutorial.cam.CamSendingApp;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Cam;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RsuValueReadingApp extends AbstractApplication<RoadSideUnitOperatingSystem> implements  CommunicationApplication {

    private final static long TIME_INTERVAL = 2 * TIME.SECOND;

    public final List<String> LDMmapa = new ArrayList<>();

    public void onStartup() {
        getLog().infoSimTime(this, "Initialize application");
        getOs().getAdHocModule().enable(new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(200)
                .create());


        getLog().infoSimTime(this, "Activated WLAN Module");

    }


    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
        V2xMessage msg = receivedV2xMessage.getMessage();
        getLog().infoSimTime(this, "RSU received a V2X message: {}", receivedV2xMessage.getMessage().getSimpleClassName());

        if (msg instanceof Cam cam) {

            try {
                Object deserializedValue = CamSendingApp.DEFAULT_OBJECT_SERIALIZATION.fromBytes(cam.getUserTaggedValue(), this.getClass().getClassLoader());
                String vehicleId = deserializedValue.toString().split(" ")[deserializedValue.toString().split(" ").length - 1];

                this.getLog().infoSimTime(this, "CAM message arrived, userTaggedValue: {}", new Object[]{CamSendingApp.DEFAULT_OBJECT_SERIALIZATION.fromBytes(cam.getUserTaggedValue(), this.getClass().getClassLoader())});

                if(!LDMmapa.contains(vehicleId)){
                    LDMmapa.add(vehicleId);
                    this.getLog().infoSimTime(this, "veiculo adicionado a vizinhos {}", new Object[]{vehicleId});
                    this.getLog().infoSimTime(this, "lista de vizinhos atuais: {}", new Object[]{LDMmapa});

                }
            } catch (ClassNotFoundException | IOException var5) {
                this.getLog().error("An error occurred", var5);
            }
        } else {
            this.getLog().infoSimTime(this, "Arrived message was not a CAM, but a {} msg from {}", new Object[]{msg.getSimpleClassName(), msg.getRouting().getSource().getSourceName()});
        }

    }

    public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgedMessage) {
    }

    public void onCamBuilding(CamBuilder camBuilder) {
    }

    public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
    }

    public void onShutdown() {
        this.getLog().infoSimTime(this, "Tear down", new Object[0]);
    }

    public void processEvent(Event event) throws Exception {
    }


}
