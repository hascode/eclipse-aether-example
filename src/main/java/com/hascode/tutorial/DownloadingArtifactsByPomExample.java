package com.hascode.tutorial;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

public class DownloadingArtifactsByPomExample {

  public static final String TARGET_LOCAL_REPOSITORY = "target/local-repository";

  public static void main(String[] args)
      throws Exception {
    File projectPomFile = Paths.get("", "pom.xml").toAbsolutePath().toFile();

    System.out.printf("loading this sample project's Maven descriptor from %s\n", projectPomFile);
    System.out.printf("local Maven repository set to %s\n",
        Paths.get("", TARGET_LOCAL_REPOSITORY).toAbsolutePath());

    RepositorySystem repositorySystem = getRepositorySystem();
    RepositorySystemSession repositorySystemSession = getRepositorySystemSession(repositorySystem);

    final DefaultModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest()
        .setPomFile(projectPomFile);

    ModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
    ModelBuildingResult modelBuildingResult = modelBuilder.build(modelBuildingRequest);

    Model model = modelBuildingResult.getEffectiveModel();
    System.out.printf("Maven model resolved: %s, parsing its dependencies..\n", model);
    model.getDependencies().forEach(d -> {
      System.out.printf("processing dependency: %s\n", d);
      Artifact artifact = new DefaultArtifact(d.getGroupId(), d.getArtifactId(), d.getType(),
          d.getVersion());
      ArtifactRequest artifactRequest = new ArtifactRequest();
      artifactRequest.setArtifact(artifact);
      artifactRequest.setRepositories(getRepositories(repositorySystem, repositorySystemSession));

      try {
        ArtifactResult artifactResult = repositorySystem
            .resolveArtifact(repositorySystemSession, artifactRequest);
        artifact = artifactResult.getArtifact();
        System.out.printf("artifact %s resolved to %s\n", artifact, artifact.getFile());
      } catch (ArtifactResolutionException e) {
        System.err.printf("error resolving artifact: %s\n", e.getMessage());
      }
    });

  }

  public static RepositorySystem getRepositorySystem() {
    DefaultServiceLocator serviceLocator = MavenRepositorySystemUtils.newServiceLocator();
    serviceLocator
        .addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    serviceLocator.addService(TransporterFactory.class, FileTransporterFactory.class);
    serviceLocator.addService(TransporterFactory.class, HttpTransporterFactory.class);

    serviceLocator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
      @Override
      public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
        System.err.printf("error creating service: %s\n", exception.getMessage());
        exception.printStackTrace();
      }
    });

    return serviceLocator.getService(RepositorySystem.class);
  }

  public static DefaultRepositorySystemSession getRepositorySystemSession(RepositorySystem system) {
    DefaultRepositorySystemSession repositorySystemSession = MavenRepositorySystemUtils
        .newSession();

    LocalRepository localRepository = new LocalRepository(TARGET_LOCAL_REPOSITORY);
    repositorySystemSession.setLocalRepositoryManager(
        system.newLocalRepositoryManager(repositorySystemSession, localRepository));

    repositorySystemSession.setRepositoryListener(new ConsoleRepositoryEventListener());

    return repositorySystemSession;
  }

  public static List<RemoteRepository> getRepositories(RepositorySystem system,
      RepositorySystemSession session) {
    return Arrays.asList(getCentralMavenRepository());
  }

  private static RemoteRepository getCentralMavenRepository() {
    return new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/")
        .build();
  }

}
