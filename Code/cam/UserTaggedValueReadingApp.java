    package org.eclipse.mosaic.app.tutorial.cam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.mosaic.app.tutorial.message.InterVehicleMsg;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Cam;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Denm;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;

public class UserTaggedValueReadingApp extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication, CommunicationApplication {

   public final List<String> vizinhos = new ArrayList<>();

   public void onStartup() {
      this.getLog().infoSimTime(this, "Set up", new Object[0]);
      ((VehicleOperatingSystem)this.getOs()).getAdHocModule().enable((new AdHocModuleConfiguration()).addRadio().channel(AdHocChannel.CCH).power(100.0D).create());
   }

   public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
      V2xMessage msg = receivedV2xMessage.getMessage();
      String rota = ((VehicleOperatingSystem)this.getOs()).getVehicleData().getRouteId();

      if (msg instanceof Cam cam) {

         try {
            Object deserializedValue = CamSendingApp.DEFAULT_OBJECT_SERIALIZATION.fromBytes(cam.getUserTaggedValue(), this.getClass().getClassLoader());
            String vehicleId = deserializedValue.toString().split(" ")[2];

            this.getLog().infoSimTime(this, "CAM message arrived, userTaggedValue: {}", new Object[]{CamSendingApp.DEFAULT_OBJECT_SERIALIZATION.fromBytes(cam.getUserTaggedValue(), this.getClass().getClassLoader())});

            if (!vizinhos.contains(vehicleId)) {
               vizinhos.add(vehicleId);
               this.getLog().infoSimTime(this, "veiculo adicionado a vizinhos {}", new Object[]{vehicleId});
               this.getLog().infoSimTime(this, "lista de vizinhos atuais: {}", new Object[]{vizinhos});

            }
         } catch (ClassNotFoundException | IOException var5) {
            this.getLog().error("An error occurred", var5);
         }
      }
      if (msg instanceof Denm denm) {

         String rota_recebida = denm.getEventRoadId();
         String extraInfo = denm.getExtendedContainer();
         String id_recebido = getID(extraInfo);
         String conteudo = retiraid(extraInfo);
         String veiculo_id = ((VehicleOperatingSystem)this.getOs()).getVehicleData() != null ? ((VehicleOperatingSystem)this.getOs()).getVehicleData().getName(): "unknown vehicle";

         double velocidade = ((VehicleOperatingSystem)this.getOs()).getVehicleData().getSpeed() ;
         double newSpeed = velocidade - 10;
         long interval = 1_000_000_000L; // 5 seconds in nanoseconds
         if (newSpeed <0 ) {newSpeed = 5;}
         if(Objects.equals(rota_recebida, rota)){
            if(id_recebido.contains(veiculo_id)) {
               // Encontra-se em excesso de velocidade ( Reduz a velocidade por 20 )
               newSpeed = velocidade - 10;
               if (newSpeed <0 ) {newSpeed = 5;}
               this.getOs().changeSpeedWithInterval(newSpeed, interval);
               this.getLog().infoSimTime(this, "Denm message arrived,: {}", msg);
               this.getLog().infoSimTime(this, "\n\n\n  {}   \n\n\n", conteudo);
               this.getLog().infoSimTime(this, "Reduziu a velocidade por 20: {}", velocidade);
            }
            else{
               // Um veículo da rota deste encontrasse em excesso de velocidade
               // Reduz a velocidade por 10
               if (newSpeed <0 ) {newSpeed = 5;}
               this.getOs().changeSpeedWithInterval(newSpeed, interval);
               this.getLog().infoSimTime(this, "Denm message arrived,: {}", msg);
               this.getLog().infoSimTime(this, "\n\n\n  Cuidado! Um veiculo encontra-se em excesso de velocidade na sua rota!   \n\n\n");
               this.getLog().infoSimTime(this, "Reduziu a velocidade por 10: {}",velocidade);
            }
         }
      }
   }
   public String retiraid(String msg) {
      int lastSpaceIndex = msg.lastIndexOf(' ');
      if (lastSpaceIndex == -1) {
         return "";
      }
      return msg.substring(0, lastSpaceIndex);
   }
   public String getID(String msg) {
      String[] parts = msg.split(" ");
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
   public String getRota(String vehicleMessage) {
      String[] parts = vehicleMessage.split(" ");
      return parts[parts.length - 2];
   }
   public void processEvent(Event event) throws Exception {
   }

   public void onVehicleUpdated(@Nullable VehicleData previousVehicleData, @Nonnull VehicleData updatedVehicleData) {
   }

}