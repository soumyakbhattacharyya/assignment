package com.acme.test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.acme.RouteBoxer;
import com.acme.model.Device;
import com.acme.model.PlannedRoute;
import com.acme.model.Reading;
import com.acme.model.Segment;
import com.acme.model.Vehicle;

@SpringBootTest
class AlgoTest {

	@Test
	void testAlgorithm() {

		// initialize
		Vehicle vehicle = createVehicle();
		// change the device reading within the Device model class

		/**
		 * initialization of planned route
		 */

		// first segment of the planned route
		RouteBoxer.LatLng l1 = new RouteBoxer().new LatLng(50.5, 30.5);
		RouteBoxer.LatLng l2 = new RouteBoxer().new LatLng(50.4, 30.6);

		// second segment of the planned route
		RouteBoxer.LatLng l3 = l2;
		RouteBoxer.LatLng l4 = new RouteBoxer().new LatLng(50.3, 30.7);

		// signifies the maximum acceptable range given a segment, beyond which if a
		// vehicle travels, it essentially counts for a deviation
		double range = 5; // kms

		Segment firstSegment = new Segment(UUID.randomUUID().toString(), l1, l2, range);
		Segment secondSegment = new Segment(UUID.randomUUID().toString(), l3, l4, range);
		List<Segment> segments = new ArrayList<>();
		segments.add(firstSegment);
		segments.add(secondSegment);

		PlannedRoute plannedRoute = new PlannedRoute(UUID.randomUUID().toString(), segments);

		/**
		 * emit reading from a device
		 */

		Reading reading = vehicle.getDevice().emitReading();

		boolean vehicleHasNotDeviated = false;
		for (Segment segment : plannedRoute.getSegments()) {
			for (RouteBoxer.LatLngBounds bound : segment.getBoundingBoxes()) {
				vehicleHasNotDeviated = vehicleHasNotDeviated || bound.contains(reading.getLatlng());
			}
		}

		if (!vehicleHasNotDeviated) {
			sendNotification();
		}else {
			System.out.println("vehicle on track");
		}

	}

	private void sendNotification() {
		System.out.println("send notification");

	}

	private Vehicle createVehicle() {
		return new Vehicle(UUID.randomUUID().toString(), "56-675-564", "A1", new Device(UUID.randomUUID().toString()));
	}
}
