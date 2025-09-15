package org.eclipse.mosaic.app.tutorial.fog;

import org.eclipse.mosaic.app.tutorial.cam.CamSendingApp;
import org.eclipse.mosaic.app.tutorial.message.InterVehicleMsg;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.*;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.OperatingSystem;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.enums.SensorType;
import org.eclipse.mosaic.lib.geo.GeoArea;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.road.IRoadPosition;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Cam;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Denm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.DenmContent;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class FogValueReadingApp extends AbstractApplication<RoadSideUnitOperatingSystem> implements  CommunicationApplication {

    private final static long TIME_INTERVAL = 2 * TIME.SECOND;

    public final List<String> RSUSmapa = new ArrayList<>();
    public final List<String> CarMap = new ArrayList<>();
    public HashMap<String,String> vehicleData = new HashMap<>();
    public void onStartup() {
        //getLog().infoSimTime(this, "Initializing Fog");
        //getOs().getAdHocModule().enable(new AdHocModuleConfiguration()
        //        .addRadio()
        //        .channel(AdHocChannel.CCH)
        //        .power(200)
        //       .create());
        getOs().getCellModule().enable(new CellModuleConfiguration());
    }

    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
        V2xMessage msg = receivedV2xMessage.getMessage();

        if (msg instanceof InterVehicleMsg ivMsg) {
            getLog().infoSimTime(this, "Received a V2X message: {}", msg);
            String senderName = ivMsg.getName();
            getLog().infoSimTime(this, "Sender Name: {}", senderName);
            String message = ivMsg.getMsgrecebida();
            getLog().infoSimTime(this, "Message {}",message);
            String veiculo_id = getVeiculoID(message);
            getLog().infoSimTime(this, "Veículo id: {}", veiculo_id);

            if(!vehicleData.containsKey(veiculo_id)) {
                vehicleData.put(veiculo_id, message);
            }
            else {
                vehicleData.replace(veiculo_id, message);
            }

            String rota = getRota(message);
            String speedlimit = getSpeedlimit(message);
            String speeding = speeding(vehicleData,rota, veiculo_id ,speedlimit);
            getLog().infoSimTime(this, "\n\n Speeding: {}", speeding);

            if(!CarMap.contains(veiculo_id)) {
                CarMap.add(veiculo_id);
                getLog().infoSimTime(this, "Veículo adicionado ao mapa de veículos {}", CarMap);
            }
            if(!RSUSmapa.contains(senderName)) {
                RSUSmapa.add(senderName);
                getLog().infoSimTime(this, "Rsus: {}", RSUSmapa);
            }

        }
    }
    public String getRoadId(String roadid){
        String[] parts = roadid.split(" ");
        return parts[parts.length-3];
    }
    public String getVeiculoID (String car) {
        String[] parts = car.split(" ");
        return parts[2];
    }
    public String getRota(String vehicleMessage) {
        String[] parts = vehicleMessage.split(" ");
        return parts[parts.length - 2];
    }
    public String getSpeedlimit(String vehicleMessage) {
        String[] parts = vehicleMessage.split(" ");
        return parts[parts.length-1];
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

    public String speeding(HashMap<String, String> vehicleData, String rota, String veiculoId, String speedlimit) {

        for (Map.Entry<String, String> entry : vehicleData.entrySet()) {
            String message  = entry.getValue();
            if (message != null && message.contains(rota)) {
                String[] parts = message.split(" ");
                String velocidade = parts[4];

                if( Double.parseDouble(velocidade) > Double.parseDouble(speedlimit) ) {
                    String roadid = getRoadId(vehicleData.get(entry.getKey()));
                    float vel = Float.parseFloat(velocidade);
                    sendDenmToRsu(roadid,vel,veiculoId);
                    return "\n\n Veiculo " + veiculoId + " desloca-se a " + velocidade + " km/h numa zona com limite de velocidade " + speedlimit + "\n\n";

                }

            }
        }
        return "";
    }

    private void sendCellModuleBroadcast(V2xMessage msgRecebida) {
        if (msgRecebida instanceof Denm denm) {
            try {

                getOs().getCellModule().sendV2xMessage(denm);
                getLog().infoSimTime(this, "Fog node sent DENM ");

            } catch (Exception e) {
                getLog().error("Erro:", e);
            }
        }
    }
    private void sendDenmToRsu( String roadId, float vehicleSpeed, String veiculoId) {
        GeoPoint fogPosition = getOs().getPosition();

        GeoPoint center = getOs().getPosition();
        double radius = 1000.0;
        GeoArea geoArea = new GeoCircle(center, radius);
        if(vehicleSpeed<65) {
            DenmContent denmContent = new DenmContent(
                    getOs().getSimulationTime(),
                    fogPosition,
                    roadId,
                    SensorType.SPEED,
                    10,
                    vehicleSpeed,
                    0.0f,
                    null,
                    null,
                    "Por favor diminua a sua velocidade! Está a por em perigo a sua segurança e a dos outros condutores. " +veiculoId

            );
            MessageRouting routing = getOs().getCellModule().createMessageRouting().geoBroadcastBasedOnUnicast(geoArea);

            Denm denm = new Denm(routing, denmContent, 0L);
            sendCellModuleBroadcast(denm);
        } else{
            DenmContent denmContent = new DenmContent(
                    getOs().getSimulationTime(),
                    fogPosition,
                    roadId,
                    SensorType.SPEED,
                    10,
                    vehicleSpeed,
                    0.0f,
                    null,
                    null,
                    "DIMINUA A SUA VELOCIDADE! VOÇÊ É UM CRIMINOSO. " + veiculoId

            );
            MessageRouting routing = getOs().getCellModule().createMessageRouting().geoBroadcastBasedOnUnicast(geoArea);

            Denm denm = new Denm(routing, denmContent, 0L);
            sendCellModuleBroadcast(denm);
        }


    }
}
