package io.metersphere.api.service;


import io.metersphere.api.dto.automation.ApiScenarioDTO;
import io.metersphere.api.dto.automation.ApiScenarioModuleDTO;
import io.metersphere.api.dto.automation.ApiScenarioRequest;
import io.metersphere.api.dto.automation.DragApiScenarioModuleRequest;
import io.metersphere.base.domain.ApiScenario;
import io.metersphere.base.domain.ApiScenarioModule;
import io.metersphere.base.domain.ApiScenarioModuleExample;
import io.metersphere.base.mapper.ApiScenarioMapper;
import io.metersphere.base.mapper.ApiScenarioModuleMapper;
import io.metersphere.base.mapper.ext.ExtApiScenarioModuleMapper;
import io.metersphere.commons.constants.TestCaseConstants;
import io.metersphere.commons.exception.MSException;
import io.metersphere.i18n.Translator;
import io.metersphere.service.NodeTreeService;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class ApiScenarioModuleService extends NodeTreeService<ApiScenarioModuleDTO> {

    @Resource
    ApiScenarioModuleMapper apiScenarioModuleMapper;
    @Resource
    ExtApiScenarioModuleMapper extApiScenarioModuleMapper;
    @Resource
    ApiAutomationService apiAutomationService;
    @Resource
    SqlSessionFactory sqlSessionFactory;

    public ApiScenarioModuleService() {
        super(ApiScenarioModuleDTO.class);
    }

    public List<ApiScenarioModuleDTO> getNodeTreeByProjectId(String projectId) {
        List<ApiScenarioModuleDTO> nodes = extApiScenarioModuleMapper.getNodeTreeByProjectId(projectId);
        return getNodeTrees(nodes);
    }

    public String addNode(ApiScenarioModule node) {
        validateNode(node);
        node.setCreateTime(System.currentTimeMillis());
        node.setUpdateTime(System.currentTimeMillis());
        node.setId(UUID.randomUUID().toString());
        apiScenarioModuleMapper.insertSelective(node);
        return node.getId();
    }

    private void validateNode(ApiScenarioModule node) {
        if (node.getLevel() > TestCaseConstants.MAX_NODE_DEPTH) {
            throw new RuntimeException(Translator.get("test_case_node_level_tip")
                    + TestCaseConstants.MAX_NODE_DEPTH + Translator.get("test_case_node_level"));
        }
        checkApiScenarioModuleExist(node);
    }

    private void checkApiScenarioModuleExist(ApiScenarioModule node) {
        if (node.getName() != null) {
            ApiScenarioModuleExample example = new ApiScenarioModuleExample();
            ApiScenarioModuleExample.Criteria criteria = example.createCriteria();
            criteria.andNameEqualTo(node.getName())
                    .andProjectIdEqualTo(node.getProjectId());
            if (StringUtils.isNotBlank(node.getParentId())) {
                criteria.andParentIdEqualTo(node.getParentId());
            } else {
                criteria.andParentIdIsNull();
            }
            if (StringUtils.isNotBlank(node.getId())) {
                criteria.andIdNotEqualTo(node.getId());
            }
            if (apiScenarioModuleMapper.selectByExample(example).size() > 0) {
                MSException.throwException(Translator.get("test_case_module_already_exists"));
            }
        }
    }

    private List<ApiScenarioDTO> queryByModuleIds(DragApiScenarioModuleRequest request) {
        ApiScenarioRequest apiScenarioRequest = new ApiScenarioRequest();
        apiScenarioRequest.setProjectId(request.getProjectId());
        apiScenarioRequest.setModuleIds(request.getNodeIds());
        return apiAutomationService.list(apiScenarioRequest);
    }

    public int editNode(DragApiScenarioModuleRequest request) {
        request.setUpdateTime(System.currentTimeMillis());
        checkApiScenarioModuleExist(request);
        List<ApiScenarioDTO> apiScenarios = queryByModuleIds(request);

        apiScenarios.forEach(apiScenario -> {
            StringBuilder path = new StringBuilder(apiScenario.getModulePath());
            List<String> pathLists = Arrays.asList(path.toString().split("/"));
            pathLists.set(request.getLevel(), request.getName());
            path.delete(0, path.length());
            for (int i = 1; i < pathLists.size(); i++) {
                path.append("/").append(pathLists.get(i));
            }
            apiScenario.setModulePath(path.toString());
        });

        batchUpdateApiScenario(apiScenarios);

        return apiScenarioModuleMapper.updateByPrimaryKeySelective(request);
    }

    public int deleteNode(List<String> nodeIds) {
        apiAutomationService.deleteByIds(nodeIds);

        ApiScenarioModuleExample apiScenarioModuleExample = new ApiScenarioModuleExample();
        apiScenarioModuleExample.createCriteria().andIdIn(nodeIds);
        return apiScenarioModuleMapper.deleteByExample(apiScenarioModuleExample);
    }

    private void batchUpdateApiScenario(List<ApiScenarioDTO> apiScenarios) {
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ApiScenarioMapper apiScenarioMapper = sqlSession.getMapper(ApiScenarioMapper.class);
        apiScenarios.forEach(apiScenarioMapper::updateByPrimaryKey);
        sqlSession.flushStatements();
    }

    public void dragNode(DragApiScenarioModuleRequest request) {

        checkApiScenarioModuleExist(request);

        List<String> nodeIds = request.getNodeIds();

        List<ApiScenarioDTO> apiScenarios = queryByModuleIds(request);

        ApiScenarioModuleDTO nodeTree = request.getNodeTree();

        List<ApiScenarioModule> updateNodes = new ArrayList<>();
        if (nodeTree == null) {
            return;
        }
        buildUpdateDefinition(nodeTree, apiScenarios, updateNodes, "/", "0", nodeTree.getLevel());

        updateNodes = updateNodes.stream()
                .filter(item -> nodeIds.contains(item.getId()))
                .collect(Collectors.toList());

        batchUpdateModule(updateNodes);

        batchUpdateApiScenario(apiScenarios);
    }

    private void buildUpdateDefinition(ApiScenarioModuleDTO rootNode, List<ApiScenarioDTO> apiScenarios,
                                       List<ApiScenarioModule> updateNodes, String rootPath, String pId, int level) {

        rootPath = rootPath + rootNode.getName();

        if (level > 8) {
            MSException.throwException(Translator.get("node_deep_limit"));
        }
        if (rootNode.getId().equals("root")) {
            rootPath = "";
        }
        ApiScenarioModule apiScenarioModule = new ApiScenarioModule();
        apiScenarioModule.setId(rootNode.getId());
        apiScenarioModule.setLevel(level);
        apiScenarioModule.setParentId(pId);
        updateNodes.add(apiScenarioModule);

        for (ApiScenario item : apiScenarios) {
            if (StringUtils.equals(item.getApiScenarioModuleId(), rootNode.getId())) {
                item.setModulePath(rootPath);
            }
        }

        List<ApiScenarioModuleDTO> children = rootNode.getChildren();
        if (children != null && children.size() > 0) {
            for (ApiScenarioModuleDTO child : children) {
                buildUpdateDefinition(child, apiScenarios, updateNodes, rootPath + '/', rootNode.getId(), level + 1);
            }
        }
    }

    private void batchUpdateModule(List<ApiScenarioModule> updateNodes) {
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ApiScenarioModuleMapper apiScenarioModuleMapper = sqlSession.getMapper(ApiScenarioModuleMapper.class);
        updateNodes.forEach(apiScenarioModuleMapper::updateByPrimaryKeySelective);
        sqlSession.flushStatements();
    }

}