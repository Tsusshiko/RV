    package org.eclipse.mosaic.app.tutorial.cam;

import java.io.IOException;
import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
import org.eclipse.mosaic.lib.enums.DriveDirection;
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.objects.road.IConnection;
import org.eclipse.mosaic.lib.objects.road.IRoadPosition;
import org.eclipse.mosaic.lib.objects.road.IWay;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.SerializationUtils;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.lib.util.scheduling.EventProcessor;

public class CamSendingApp extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication, CommunicationApplication {
   public static final SerializationUtils<CamSendingApp.MyComplexTaggedValue> DEFAULT_OBJECT_SERIALIZATION = new SerializationUtils();

   public void onStartup() {
      ((VehicleOperatingSystem)this.getOs()).getAdHocModule().enable((new AdHocModuleConfiguration()).camMinimalPayloadLength(200L).addRadio().channel(AdHocChannel.CCH).power(50.0D).create());
      this.getLog().infoSimTime(this, "Set up", new Object[0]);
      ((VehicleOperatingSystem)this.getOs()).getEventManager().addEvent(((VehicleOperatingSystem)this.getOs()).getSimulationTime() + 100000000L, new EventProcessor[]{this});
   }

   public void processEvent(Event event) {
      this.sendCam();
      ((VehicleOperatingSystem)this.getOs()).getEventManager().addEvent(((VehicleOperatingSystem)this.getOs()).getSimulationTime() + 100000000L, new EventProcessor[]{this});
   }

   private void sendCam() {
      this.getLog().infoSimTime(this, "Sending CAM", new Object[0]);
      ((VehicleOperatingSystem)this.getOs()).getAdHocModule().sendCam();
   }

   public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
   }

   public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgedMessage) {
   }

   public void onCamBuilding(CamBuilder camBuilder) {
      CamSendingApp.MyComplexTaggedValue exampleContent = new CamSendingApp.MyComplexTaggedValue();

      String veiculo_id = ((VehicleOperatingSystem)this.getOs()).getVehicleData() != null ? ((VehicleOperatingSystem)this.getOs()).getVehicleData().getName() : "unknown vehicle";
      CartesianPoint posicao_projetada = ((VehicleOperatingSystem)this.getOs()).getVehicleData().getProjectedPosition() ;
      String rota = ((VehicleOperatingSystem)this.getOs()).getVehicleData().getRouteId();
      IRoadPosition roadPosition = ((VehicleOperatingSystem)this.getOs()).getVehicleData().getRoadPosition();
      String segmentId = roadPosition.getConnectionId();
      IConnection conn = roadPosition.getConnection();
      IWay way = conn.getWay();
      double speedLimit = way.getMaxSpeedInKmh();
      double velocidade = ((VehicleOperatingSystem)this.getOs()).getVehicleData().getSpeed() ;
      double acelaracao = ((VehicleOperatingSystem)this.getOs()).getVehicleData().getLongitudinalAcceleration();
      double brake = ((VehicleOperatingSystem)this.getOs()).getVehicleData().getBrake();
      double direcao = ((VehicleOperatingSystem)this.getOs()).getVehicleData().getHeading();

      exampleContent.fooString = "Hello from " + veiculo_id + " velocidade " + velocidade + " acelaracao " + acelaracao + " paragem " + brake + " direcao " + direcao + " posicao " + posicao_projetada + "roadid"+ roadPosition + " rota " + rota + " speedlimit " + speedLimit;

      try {
         byte[] byteArray = DEFAULT_OBJECT_SERIALIZATION.toBytes(exampleContent);
         camBuilder.userTaggedValue(byteArray);
      } catch (IOException var4) {
         this.getLog().error("Error during a serialization.", var4);
      }

   }

   public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
   }

   public void onShutdown() {
      this.getLog().infoSimTime(this, "Tear down", new Object[0]);
   }

   public void onVehicleUpdated(@Nullable VehicleData previousVehicleData, @Nonnull VehicleData updatedVehicleData) {
   }

   private static class MyComplexTaggedValue implements Serializable {

      public String fooString;

      public String toString() {
         return "CAM:" + this.fooString;
      }
   }
}