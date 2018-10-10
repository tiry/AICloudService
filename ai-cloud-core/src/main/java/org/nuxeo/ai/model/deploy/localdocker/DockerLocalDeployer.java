package org.nuxeo.ai.model.deploy.localdocker;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.nuxeo.ai.model.deploy.ModelDeployer;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.netty.DockerCmdExecFactoryImpl;

public class DockerLocalDeployer implements ModelDeployer {

	protected DockerClientConfig config;
	
	protected DockerClient dockerClient;
		
	protected final AtomicInteger nextAvailablePort = new AtomicInteger(9090);
	
	@Override
	public String getName() {
		return "localDocker";
	}

	public DockerLocalDeployer() {
		
		config = DockerClientConfig.createDefaultConfigBuilder()
//				  .withDockerHost("tcp://127.0.0.1:2376")
				  .withDockerTlsVerify(false)
				  .withDockerCertPath("/home/tiry/.docker")
//				  .withRegistryUsername(registryUser)
//				  .withRegistryPassword(registryPass)
//				  .withRegistryEmail(registryMail)
//				  .withRegistryUrl(registryUrl)
				  .build();

				DockerCmdExecFactory dockerCmdExecFactory = new DockerCmdExecFactoryImpl();
//				  .withReadTimeout(1000)
//				  .withConnectTimeout(1000)
//				  .withMaxTotalConnections(100)
//				  .withMaxPerRouteConnections(10);

				
				dockerClient = DockerClientBuilder.getInstance(config)
				  .withDockerCmdExecFactory(dockerCmdExecFactory)
				  .build();
	}
	
	protected String[] getEnv(String modelUUID, URI blobModel) {
		return new String[] {};
	}
	
	
	protected ExposedPort getExposedPort() {
		ExposedPort tcp80 = ExposedPort.tcp(80);
		return tcp80;
	}
	
	protected String getCmd() {
		return null;
	}
	
	protected String getImageName() {
		return "httpd";
	}
	
	@Override
	public URI deployModel(String modelUUID, URI blobModel) {
		
		ExposedPort exposedPort = getExposedPort();
		Ports portBindings = new Ports();		
		//portBindings.bind(tcp8080, Binding.bindPortRange(8090, 9090));
		int port = nextAvailablePort.getAndIncrement();
		portBindings.bind(exposedPort, Binding.bindPort(port));
		
		Map<String, String> labels = new HashMap<>();
		labels.put("modelUUID", modelUUID);
		

		CreateContainerCmd createCmd = dockerClient.createContainerCmd(getImageName())
//				   .withCmd("sleep", "9999")
				   .withName("tf-" + modelUUID)
				   .withLabels(labels)
				   .withExposedPorts(exposedPort)
				   .withPortBindings(portBindings)
				   .withEnv(getEnv(modelUUID, blobModel));
		
		String cmd = getCmd();
		if (cmd!=null) {
			createCmd = createCmd.withCmd(cmd);
		}
		
		CreateContainerResponse tfcontainer = createCmd.exec();

		StartContainerCmd start = dockerClient.startContainerCmd(tfcontainer.getId());		
		start.exec();
				
		return getModelEndPoint(modelUUID);	
	}

	protected URI buildEndPointURI(int port) {		
		try {
			return new URI("http", "//127.0.0.1:" + port, null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}	
	
	@Override
	public URI getModelEndPoint(String modelUUID) {

		Map<String, String> labels = new HashMap<>();
		labels.put("modelUUID", modelUUID);

		List<Container> containers = dockerClient.listContainersCmd().withLabelFilter(labels).exec();

		if (containers.size()==0) {
			// need to redeploy ?
			return null;
		} else if (containers.size()==1) {
			ContainerPort[] ports = containers.get(0).getPorts();
			for (ContainerPort port: ports) {
				if (port.getPublicPort()!=null) {
					return buildEndPointURI(port.getPublicPort().intValue());
				}
			}
			return null;
			
		} else {
			// Yurk !
			return null;
		}
	}

	@Override
	public void undeployModel(String modelUUID) {
		Map<String, String> labels = new HashMap<>();
		labels.put("modelUUID", modelUUID);

		List<Container> containers = dockerClient.listContainersCmd().withLabelFilter(labels).exec();
		
		if (containers.size()==0) {
			// dead ?
		} else if (containers.size()==1) {
			dockerClient.stopContainerCmd(containers.get(0).getId()).exec();
		} else {
			// Yurk : kill everyone ?
			// XXX
		}

	}

}
