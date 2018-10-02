package org.nuxeo.ai;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ai.service.AICloudService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.PartialDeploy;
import org.nuxeo.runtime.test.runner.TargetExtensions;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("org.nuxeo.ai.ai-cloud-core")
@PartialDeploy(bundle = "studio.extensions.nuxeo-ai-online-services", extensions = { TargetExtensions.ContentModel.class })
public class TestAICloudService {

    @Inject
    protected AICloudService aicloudservice;

    @Test
    public void testService() {
        assertNotNull(aicloudservice);
    }
}
