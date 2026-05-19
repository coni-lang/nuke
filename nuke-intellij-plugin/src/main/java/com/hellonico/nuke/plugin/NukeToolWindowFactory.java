package com.hellonico.nuke.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ScriptRunnerUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ActionToolbar;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NukeToolWindowFactory implements ToolWindowFactory {

    private static final Map<Project, Tree> taskTrees = new ConcurrentHashMap<>();
    private static final Map<Project, DefaultMutableTreeNode> tasksNodes = new ConcurrentHashMap<>();

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        JPanel panel = new JPanel(new BorderLayout());
        
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new NukeSyncAction());
        actionGroup.add(new NukeImportGradleAction());
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("NukeToolbar", actionGroup, true);
        toolbar.setTargetComponent(panel);
        panel.add(toolbar.getComponent(), BorderLayout.NORTH);

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Project: " + project.getName());
        DefaultMutableTreeNode tasksNode = new DefaultMutableTreeNode("Lifecycle");
        rootNode.add(tasksNode);
        
        Tree taskTree = new Tree(rootNode);
        
        taskTrees.put(project, taskTree);
        tasksNodes.put(project, tasksNode);

        taskTree.setRootVisible(true);
        taskTree.setShowsRootHandles(true);
        
        taskTree.setCellRenderer(new ColoredTreeCellRenderer() {
            @Override
            public void customizeCellRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();
                if (userObject instanceof String) {
                    String text = (String) userObject;
                    if (text.startsWith("Project: ")) {
                        append(text.substring(9), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                        setIcon(AllIcons.Nodes.Module);
                    } else if (text.equals("Lifecycle")) {
                        append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                        setIcon(AllIcons.Nodes.ConfigFolder);
                    } else {
                        // It's a task
                        append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                        setIcon(AllIcons.Nodes.Plugin);
                    }
                }
            }
        });

        taskTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Tree currentTree = taskTrees.get(project);
                    if (currentTree == null) return;
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) currentTree.getLastSelectedPathComponent();
                    if (node != null && node.isLeaf() && node.getParent() != null && "Lifecycle".equals(((DefaultMutableTreeNode)node.getParent()).getUserObject())) {
                        String taskName = ((String) node.getUserObject()).split(" - ")[0].trim();
                        runTask(project, taskName);
                    }
                }
            }
        });
        
        panel.add(new JBScrollPane(taskTree), BorderLayout.CENTER);
        
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
        
        refresh(project);
    }

    public static void refresh(Project project) {
        Tree taskTree = taskTrees.get(project);
        DefaultMutableTreeNode tasksNode = tasksNodes.get(project);
        if (taskTree == null || tasksNode == null) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String basePath = project.getBasePath();
                if (basePath == null) return;
                
                GeneralCommandLine cmd = new GeneralCommandLine(NukeProjectManager.getNukeExecutable(), "tasks");
                cmd.setWorkDirectory(basePath);
                
                String output = ScriptRunnerUtil.getProcessOutput(cmd);
                List<String> tasks = new ArrayList<>();
                for (String line : output.split("\\r?\\n")) {
                    line = line.trim();
                    if (line.startsWith("Available Tasks:")) continue;
                    if (line.isEmpty()) continue;
                    tasks.add(line);
                }
                
                ApplicationManager.getApplication().invokeLater(() -> {
                    tasksNode.removeAllChildren();
                    for (String t : tasks) {
                        tasksNode.add(new DefaultMutableTreeNode(t));
                    }
                    ((DefaultTreeModel) taskTree.getModel()).reload();
                    for (int i = 0; i < taskTree.getRowCount(); i++) {
                        taskTree.expandRow(i);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void runTask(Project project, String taskName) {
        String basePath = project.getBasePath();
        if (basePath == null) return;
        
        RunManager runManager = RunManager.getInstance(project);
        ConfigurationFactory factory = new NukeRunConfigurationType().getConfigurationFactories()[0];
        
        RunnerAndConfigurationSettings settings = runManager.createConfiguration("Nuke " + taskName, factory);
        NukeRunConfiguration config = (NukeRunConfiguration) settings.getConfiguration();
        config.setTaskName(taskName);
        
        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);
        
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }
}
