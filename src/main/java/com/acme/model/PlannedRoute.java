package com.acme.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class PlannedRoute {

	private String id;
	private List<Segment> segments = new ArrayList<>();
	

}
