package org.etg.espresso.templates.kotlin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.velocity.VelocityContext;
import org.etg.ETGProperties;
import org.etg.espresso.codegen.actions.ActionCodeMapper;
import org.etg.espresso.codegen.actions.ActionCodeMapperFactory;
import org.etg.espresso.templates.VelocityTemplate;
import org.etg.espresso.templates.VelocityTemplateConverter;
import org.etg.mate.models.Action;
import org.etg.mate.models.ActionType;
import org.etg.mate.models.Widget;

import java.util.ArrayList;
import java.util.List;

public class CuidarSetupCodeTemplate implements VelocityTemplate {

    private static final String NULL = "null";

    private final List<Action> setupCodeWidgetActions;
    private final ETGProperties etgProperties;

    public CuidarSetupCodeTemplate(List<Action> setupCodeWidgetActions, ETGProperties etgProperties) {
        if (setupCodeWidgetActions.isEmpty()) {
            throw new Error("CuidarSetupCodeTemplate with empty setup code widget actions");
        }
        this.setupCodeWidgetActions = setupCodeWidgetActions;
        this.etgProperties = etgProperties;
    }

    @Override
    public String getFileName() {
        return "";
    }

    @Override
    public String getRelativePath() {
        return "";
    }

    public String getAsRawString() {
        return "UserAuthorization(\"${dni}\", \"${gender}\", \"${identificacion}\")\n" +
                "AppAuthenticator.setAccessToken(\"${accessToken}\")\n" +
                "PreferencesManager.saveRefreshToken(\"${refreshToken}\")\n" +
                "PreferencesManager.savePassword(\"${password}\")\n" +
                "\n" +
                "val localAddress = LocalAddress(\"${province}\", \"${locality}\",\n" +
                "        \"${apartment}\", \"${street}\", \"${number}\", ${floor}, ${door},\n" +
                "        \"${postalCode}\", ${others})\n" +
                "val localLocation = LocalLocation(\"${latitude}\", \"${longitude}\")\n" +
                "val localCoep = LocalCoep(\"${coepName}\", \"${contactInformation}\")\n" +
                "\n" +
                "val localState = LocalState(${userStatus}, \"${expirationDate}\", ${coep},\n" +
                "        ${pims})\n" +
                "\n" +
                "val localUser = LocalUser(${dni}, \"${gender}\", \"${birthDate}\", \"${names}\",\n" +
                "        \"${lastNames}\", \"${phone}\", ${address}, ${location},\n" +
                "        ${currentState})\n" +
                "EncryptedDataBase.instance.userDao.insert(localUser).blockingAwait()\n" +
                "\n" +
                "${mockServerResponses}\n";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof CuidarSetupCodeTemplate;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }


    public String getAsString() throws Exception {
        VelocityContext velocityContext = buildVelocityContext();
        VelocityTemplateConverter templateConverter = new VelocityTemplateConverter(velocityContext);
        return templateConverter.applyContextToTemplate(this);
    }

    private VelocityContext buildVelocityContext() throws Exception {
        VelocityContext velocityContext = new VelocityContext();

        List<String> mockServerResponseEspressoLines = buildMockServerResponseEspressoLines();
        JsonNode userInfoJSON = buildJSONFromEspresoLines(mockServerResponseEspressoLines, "dni");
        JsonNode authJSON = buildJSONFromEspresoLines(mockServerResponseEspressoLines, "token");

        // user stuff
        velocityContext.put("dni", userInfoJSON.get("dni").asText());
        velocityContext.put("gender", userInfoJSON.get("sexo").asText());
        velocityContext.put("identificacion", parseNroIdentificacion());
        velocityContext.put("birthDate", userInfoJSON.get("fecha-nacimiento").asText());
        velocityContext.put("names", userInfoJSON.get("nombres").asText());
        velocityContext.put("lastNames", userInfoJSON.get("apellidos").asText());
        velocityContext.put("phone", userInfoJSON.get("telefono").asText());
        velocityContext.put("address", "localAddress");
        velocityContext.put("location", "localLocation");
        velocityContext.put("currentState", "localState");

        // auth stuff
        velocityContext.put("refreshToken", authJSON.get("refresh_token").asText());
        velocityContext.put("accessToken", authJSON.get("token").asText());
        velocityContext.put("password", "password");

        // address stuff
        velocityContext.put("province", userInfoJSON.get("domicilio").get("provincia").asText());
        velocityContext.put("locality", userInfoJSON.get("domicilio").get("localidad").asText());
        velocityContext.put("apartment", userInfoJSON.get("domicilio").get("departamento").asText());
        velocityContext.put("street", userInfoJSON.get("domicilio").get("calle").asText());
        velocityContext.put("number", userInfoJSON.get("domicilio").get("numero").asText());
        velocityContext.put("floor", NULL);
        velocityContext.put("door", NULL);
        velocityContext.put("postalCode", userInfoJSON.get("domicilio").get("codigo-postal").asText());
        velocityContext.put("others", NULL);

        // location stuff
        velocityContext.put("latitude", "");
        velocityContext.put("longitude", "");

        // coep stuff
        velocityContext.put("coepName", "");
        velocityContext.put("contactInformation", "");

        // user state stuff
        velocityContext.put("userStatus", mapNombreEstadoToUserStatus(userInfoJSON.get("estado-actual").get("nombre-estado").asText()));
        JsonNode fechaVencimientoEstadoActual = userInfoJSON.get("estado-actual").get("fecha-hora-vencimiento");
        velocityContext.put("expirationDate", fechaVencimientoEstadoActual == null ?
                NULL : fechaVencimientoEstadoActual.asText());
        velocityContext.put("coep", "localCoep");
        velocityContext.put("pims", NULL);

        // mock server responses
        velocityContext.put("mockServerResponses", String.join("\n", mockServerResponseEspressoLines));

        return velocityContext;
    }

    private JsonNode buildJSONFromEspresoLines(List<String> mockServerResponseEspressoLines, String match) throws Exception {
        String espressoLinePrefix = "webServer.enqueue(MockResponse().setResponseCode(200).setBody(\"";
        String espressoLineSufix = "\"))";

        int networkingInfoIndex = -1;
        for (int i = 0, mockServerResponseEspressoLinesSize = mockServerResponseEspressoLines.size(); i < mockServerResponseEspressoLinesSize; i++) {
            String line = mockServerResponseEspressoLines.get(i);
            if (line.contains(match)) {
                networkingInfoIndex = i;
                break;
            }
        }

        if (networkingInfoIndex == -1) {
            throw new Exception("Unable to find token info in LOGIN action's networking info");
        }

        String line = mockServerResponseEspressoLines.get(networkingInfoIndex);
        String jsonContent = line.substring(espressoLinePrefix.length(), line.length() - espressoLineSufix.length());
        jsonContent = jsonContent.replace("\\\"", "\"");

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(jsonContent);
    }

    private String parseNroIdentificacion() throws Exception {
        for (Action action : setupCodeWidgetActions) {
            Widget widget = action.getWidget();
            if (action.getActionType() == ActionType.TYPE_TEXT && widget.getId().contains("numero_tramite_identificacion")) {
                return action.getExtraInfo();
            }
        }

        throw new Exception("Unable to find nro identificacion in setup code widget actions");
    }

    private String mapNombreEstadoToUserStatus(String nombre) {
        switch (nombre) {
            case "DEBE_AUTODIAGNOSTICARSE":
                return "UserStatus.MUST_SELF_DIAGNOSE";
            case "NO_INFECTADO":
                return "UserStatus.NOT_INFECTED";
            case "NO_CONTAGIOSO":
                return "UserStatus.NOT_CONTAGIOUS";
            case "INFECTADO":
                return "UserStatus.INFECTED";
            case "DERIVADO_A_SALUD_LOCAL":
                return "UserStatus.DERIVED_TO_LOCAL_HEALTH";
            case "UNKNOWN":
            default:
                return "UserStatus.UNKNOWN";
        }
    }

    private List<String> buildMockServerResponseEspressoLines() throws Exception {
        List<String> espressoLines = new ArrayList<>();
        for (Action action : setupCodeWidgetActions) {
            espressoLines.addAll(buildMockServerEspressoLinesForAction(action));
        }

        return espressoLines;
    }

    private List<String> buildMockServerEspressoLinesForAction(Action action) {
        Action mockedServerResponseAction = new Action(ActionType.MOCK_SERVER_RESPONSE);
        mockedServerResponseAction.setNetworkingInfo(action.getNetworkingInfo());
        ActionCodeMapper actionCodeMapper = ActionCodeMapperFactory.get(etgProperties, mockedServerResponseAction);
        List<String> mockServerResponseEspressoLines = new ArrayList<>();
        actionCodeMapper.addTestCodeLines(mockServerResponseEspressoLines, null, 0, 0);
        return mockServerResponseEspressoLines;
    }

    public List<String> getExtraImports() {
        List<String> imports = new ArrayList<>();
        imports.add("ar.gob.coronavirus.data.UserStatus");
        imports.add("ar.gob.coronavirus.data.local.EncryptedDataBase");
        imports.add("ar.gob.coronavirus.data.local.modelo.*");
        imports.add("ar.gob.coronavirus.data.remoto.AppAuthenticator");
        imports.add("ar.gob.coronavirus.data.remoto.modelo.UserAuthorization");
        imports.add("ar.gob.coronavirus.utils.PreferencesManager");
        return imports;
    }
}
