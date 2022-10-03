package io.ballerina.graphql.idl.client;

import io.ballerina.graphql.cmd.GraphqlProject;
import io.ballerina.graphql.cmd.pojo.Config;
import io.ballerina.graphql.cmd.pojo.Default;
import io.ballerina.graphql.cmd.pojo.Endpoints;
import io.ballerina.graphql.cmd.pojo.Extension;
import io.ballerina.graphql.cmd.pojo.Project;
import io.ballerina.graphql.exception.CmdException;
import io.ballerina.graphql.exception.ParseException;
import io.ballerina.projects.plugins.IDLSourceGeneratorContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.Location;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.graphql.cmd.Constants.MESSAGE_FOR_EMPTY_CONFIGURATION_FILE;
import static io.ballerina.graphql.cmd.Constants.MESSAGE_FOR_INVALID_CONFIGURATION_FILE_CONTENT;
import static io.ballerina.graphql.cmd.Constants.MESSAGE_FOR_INVALID_CONFIGURATION_FILE_EXTENSION;
import static io.ballerina.graphql.cmd.Constants.YAML_EXTENSION;
import static io.ballerina.graphql.cmd.Constants.YML_EXTENSION;
import static io.ballerina.graphql.generator.CodeGeneratorConstants.ROOT_PROJECT_NAME;

/**
 * This class is used to generate utility functions in the IDL client generation file.
 */
public class Utils {
    public static Constructor getProcessedConstructor() {
        Constructor constructor = new Constructor(Config.class);

        TypeDescription endpointsDesc = new TypeDescription(Endpoints.class);
        endpointsDesc.substituteProperty("default", Default.class,
                "getDefaultName", "setDefaultName");
        constructor.addTypeDescription(endpointsDesc);

        return constructor;
    }

    public static List<GraphqlProject> populateProjects(Config config, String targetOutputPath) {
        List<GraphqlProject> graphqlProjects = new ArrayList<>();
        String schema = config.getSchema();
        List<String> documents = config.getDocuments();
        Extension extensions = config.getExtensions();
        Map<String, Project> projects = config.getProjects();

        if (schema != null || documents != null || extensions != null) {
            graphqlProjects.add(new GraphqlProject(ROOT_PROJECT_NAME, schema, documents, extensions,
                    targetOutputPath));
        }

        if (projects != null) {
            for (String projectName : projects.keySet()) {
                graphqlProjects.add(new GraphqlProject(projectName,
                        projects.get(projectName).getSchema(),
                        projects.get(projectName).getDocuments(),
                        projects.get(projectName).getExtensions(),
                        targetOutputPath));
            }
        }
        return graphqlProjects;
    }

    public static Config readConfig(String filePath) throws FileNotFoundException, ParseException, CmdException {
        try {
            if (filePath.endsWith(YAML_EXTENSION) || filePath.endsWith(YML_EXTENSION)) {
                InputStream inputStream = new FileInputStream(new File(filePath));
                Constructor constructor = getProcessedConstructor();
                Yaml yaml = new Yaml(constructor);
                Config config = yaml.load(inputStream);
                if (config == null) {
                    throw new ParseException(MESSAGE_FOR_EMPTY_CONFIGURATION_FILE);
                }
                return config;
            } else {
                throw new CmdException(MESSAGE_FOR_INVALID_CONFIGURATION_FILE_EXTENSION);
            }
        } catch (YAMLException e) {
            throw new ParseException(MESSAGE_FOR_INVALID_CONFIGURATION_FILE_CONTENT + e.getMessage());
        }
    }

    public static void reportDiagnostic(IDLSourceGeneratorContext context, Constants.DiagnosticMessages error,
                                         Location location) {
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(error.getCode(), error.getDescription(),
                error.getSeverity());
        Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(diagnosticInfo, location);
        context.reportDiagnostic(diagnostic);
    }
}
