package com.hascode.tutorial;

import java.io.File;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingResult;

public class ReadingAPomExample {

  public static void main(String[] args)
      throws Exception {
    File pomFile = new File("/tmp/test/pom.xml");
    File local = new File("/tmp/test/m2");

    final DefaultModelBuildingRequest request = new DefaultModelBuildingRequest()
        .setPomFile(pomFile);

    ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
    ModelBuildingResult result = builder.build(request);

    Model model = result.getEffectiveModel();
    System.out.println("model: " + model);
    model.getDependencies().forEach(d -> {
      System.out.printf("processing dependency: %s\n", d);
    });

  }
}
