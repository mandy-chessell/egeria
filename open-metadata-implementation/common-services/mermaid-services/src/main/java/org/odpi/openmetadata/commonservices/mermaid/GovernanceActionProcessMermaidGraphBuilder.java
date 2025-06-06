/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */

package org.odpi.openmetadata.commonservices.mermaid;

import org.odpi.openmetadata.frameworks.governanceaction.properties.*;
import org.odpi.openmetadata.frameworks.openmetadata.enums.EngineActionStatus;
import org.odpi.openmetadata.frameworks.openmetadata.types.OpenMetadataProperty;

import java.util.*;


public class GovernanceActionProcessMermaidGraphBuilder extends MermaidGraphBuilderBase
{
    /**
     * Constructor for the graph
     *
     * @param processGraph contents
     */
    public GovernanceActionProcessMermaidGraphBuilder(GovernanceActionProcessGraph processGraph)
    {
        mermaidGraph.append("---\n");
        mermaidGraph.append("title: Process - ");
        mermaidGraph.append(processGraph.getGovernanceActionProcess().getProcessProperties().getDisplayName());
        mermaidGraph.append(" [");
        mermaidGraph.append(processGraph.getGovernanceActionProcess().getElementHeader().getGUID());
        mermaidGraph.append("]\n---\nflowchart LR\n%%{init: {\"flowchart\": {\"htmlLabels\": false}} }%%\n\n");

        String currentNodeName = processGraph.getGovernanceActionProcess().getElementHeader().getGUID();
        String currentDisplayName = processGraph.getGovernanceActionProcess().getProcessProperties().getDisplayName();

        appendNewMermaidNode(currentNodeName,
                             currentDisplayName,
                             processGraph.getGovernanceActionProcess().getElementHeader().getType().getTypeName(),
                             getVisualStyleForEntity(processGraph.getGovernanceActionProcess().getElementHeader(), VisualStyle.GOVERNANCE_ACTION_PROCESS));

        this.addDescription(processGraph.getGovernanceActionProcess());

        if (processGraph.getFirstProcessStep() != null)
        {
            currentNodeName    = processGraph.getFirstProcessStep().getElement().getElementHeader().getGUID();
            currentDisplayName = processGraph.getFirstProcessStep().getElement().getProcessStepProperties().getDisplayName();

            if (processGraph.getFirstProcessStep().getElement().getProcessStepProperties().getActionStatus() == EngineActionStatus.FAILED)
            {
                appendNewMermaidNode(currentNodeName,
                                     currentDisplayName,
                                     processGraph.getFirstProcessStep().getElement().getElementHeader().getType().getTypeName(),
                                     this.getAdditionalProcessStepProperties(processGraph.getFirstProcessStep().getElement()),
                                     VisualStyle.FAILED_GOVERNANCE_ACTION_PROCESS_STEP);
            }
            else
            {
                appendNewMermaidNode(currentNodeName,
                                     currentDisplayName,
                                     processGraph.getFirstProcessStep().getElement().getElementHeader().getType().getTypeName(),
                                     this.getAdditionalProcessStepProperties(processGraph.getFirstProcessStep().getElement()),
                                     VisualStyle.GOVERNANCE_ACTION_PROCESS_STEP);
            }
        }

        if (processGraph.getNextProcessSteps() != null)
        {
            for (GovernanceActionProcessStepExecutionElement node : processGraph.getNextProcessSteps())
            {
                if (node != null)
                {
                    currentNodeName    = node.getElementHeader().getGUID();
                    currentDisplayName = node.getProcessStepProperties().getDisplayName();

                    if (currentDisplayName == null)
                    {
                        currentDisplayName = node.getProcessStepProperties().getProcessStepName();
                    }

                    if (node.getProcessStepProperties().getActionStatus() == EngineActionStatus.FAILED)
                    {
                        appendNewMermaidNode(currentNodeName,
                                             currentDisplayName,
                                             node.getElementHeader().getType().getTypeName(),
                                             this.getAdditionalProcessStepProperties(node),
                                             VisualStyle.FAILED_GOVERNANCE_ACTION_PROCESS_STEP);
                    }
                    else
                    {
                        appendNewMermaidNode(currentNodeName,
                                             currentDisplayName,
                                             node.getElementHeader().getType().getTypeName(),
                                             this.getAdditionalProcessStepProperties(node),
                                             VisualStyle.GOVERNANCE_ACTION_PROCESS_STEP);
                    }
                }
            }

            for (NextGovernanceActionProcessStepLink line : processGraph.getProcessStepLinks())
            {
                if (line != null)
                {
                    super.appendMermaidLine(null,
                                            line.getPreviousProcessStep().getGUID(),
                                            line.getGuard(),
                                            line.getNextProcessStep().getGUID());
                }
            }
        }
    }

    /**
     * Extract some additional properties for a process step.
     *
     * @param processStep retrieved process step
     * @return map of additional properties
     */
    private Map<String, String> getAdditionalProcessStepProperties(GovernanceActionProcessStepExecutionElement processStep)
    {
        if ((processStep != null) && (processStep.getProcessStepProperties() != null))
        {
            Map<String, String> additionalProperties = new HashMap<>();

            if (processStep.getProcessStepProperties().getActionStatus() != null)
            {
                additionalProperties.put(OpenMetadataProperty.ACTION_STATUS.name,
                                         processStep.getProcessStepProperties().getActionStatus().getName());
            }
            if ((processStep.getProcessStepProperties().getCompletionGuards() != null) &&
                (! processStep.getProcessStepProperties().getCompletionGuards().isEmpty()))
            {
                additionalProperties.put(OpenMetadataProperty.COMPLETION_GUARDS.name,
                                         processStep.getProcessStepProperties().getCompletionGuards().toString());
            }
            if (processStep.getProcessStepProperties().getCompletionMessage() != null)
            {
                additionalProperties.put(OpenMetadataProperty.COMPLETION_MESSAGE.name,
                                         processStep.getProcessStepProperties().getCompletionMessage());
            }

            return additionalProperties;
        }

        return null;
    }

    /**
     * Add a text boxes with the description of the process (if any), followed by the request parameters (if any),
     * followed by the process itself, followed by the acton targets (if any).
     *
     * @param governanceActionProcessElement element with the potential description
     */
    private void addDescription(GovernanceActionProcessElement governanceActionProcessElement)
    {
        if (governanceActionProcessElement.getPredefinedActionTargets() != null)
        {
            for (PredefinedActionTarget predefinedActionTarget : governanceActionProcessElement.getPredefinedActionTargets())
            {
                if (predefinedActionTarget != null)
                {
                    String actionTargetNodeName = UUID.randomUUID().toString();

                    appendNewMermaidNode(actionTargetNodeName,
                                         predefinedActionTarget.getActionTargetElementStub().getUniqueName(),
                                         predefinedActionTarget.getActionTargetElementStub().getType().getTypeName(),
                                         VisualStyle.ACTION_TARGET);

                    String lineGUID = UUID.randomUUID().toString();

                    super.appendMermaidThinLine(lineGUID,
                                                governanceActionProcessElement.getElementHeader().getGUID(),
                                                predefinedActionTarget.getActionTargetName(),
                                                actionTargetNodeName);
                }
            }
        }

        String currentNodeName = governanceActionProcessElement.getElementHeader().getGUID();

        if (governanceActionProcessElement.getPredefinedRequestParameters() != null)
        {
            String parametersNodeName = UUID.randomUUID().toString();

            appendNewMermaidNode(parametersNodeName,
                                 governanceActionProcessElement.getPredefinedRequestParameters().toString(),
                                 OpenMetadataProperty.REQUEST_PARAMETERS.name,
                                 VisualStyle.REQUEST_PARAMETERS);

            super.appendInvisibleMermaidLine(parametersNodeName, currentNodeName);

            currentNodeName = parametersNodeName;
        }

        if (governanceActionProcessElement.getProcessProperties() != null)
        {
            if (governanceActionProcessElement.getProcessProperties().getDescription() != null)
            {
                String descriptionNodeName = UUID.randomUUID().toString();

                appendNewMermaidNode(descriptionNodeName,
                                     governanceActionProcessElement.getProcessProperties().getDescription(),
                                     "Description",
                                     VisualStyle.DESCRIPTION);

                super.appendInvisibleMermaidLine(descriptionNodeName, currentNodeName);
            }
        }
    }
}
