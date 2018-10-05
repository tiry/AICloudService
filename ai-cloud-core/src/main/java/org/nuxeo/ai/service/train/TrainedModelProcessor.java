/*
 *
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication. *
 *
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ai.service.train;

import java.util.Collections;
import java.util.Map;

import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since 10.3
 */
public class TrainedModelProcessor implements StreamProcessorTopology {

	@Override
	public Topology getTopology(final Map<String, String> options) {

		return Topology.builder().addComputation(() -> new AITrainedModelComputation("aiTrainResults", options),
				Collections.singletonList("i1:aiTrainResults")).build();
	}

}
