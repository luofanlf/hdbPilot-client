# GitHub Actions 权限设置指南

## 解决 403 权限错误

### 1. 检查仓库权限设置

#### 步骤 1: 进入仓库设置
1. 打开你的GitHub仓库
2. 点击 `Settings` 标签页
3. 在左侧菜单中点击 `Actions` → `General`

#### 步骤 2: 设置 Workflow permissions
在 "Workflow permissions" 部分：
- ✅ 选择 **"Read and write permissions"**
- ✅ 勾选 **"Allow GitHub Actions to create and approve pull requests"**

![Workflow Permissions](https://docs.github.com/assets/cb-40020/images/help/actions/actions-workflow-permissions.png)

### 2. 检查分支保护规则

#### 步骤 1: 进入分支设置
1. 在 `Settings` 中点击 `Branches`
2. 找到 `main` 分支的保护规则

#### 步骤 2: 检查保护规则
确保以下设置：
- ❌ **不要** 勾选 "Restrict pushes that create files"
- ❌ **不要** 勾选 "Restrict deletions"
- ✅ 允许 Actions 写入

### 3. 验证 GITHUB_TOKEN

#### 检查当前配置
在你的 workflow 文件中，确保使用：
```yaml
env:
  GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

#### 创建 Personal Access Token (如果需要)
如果问题仍然存在，可以创建 PAT：

1. 进入 GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. 点击 "Generate new token (classic)"
3. 选择以下权限：
   - `repo` (完整的仓库访问权限)
   - `workflow` (工作流权限)
4. 复制生成的token
5. 在仓库中添加新的 secret：
   - 名称：`PAT_TOKEN`
   - 值：你生成的token
6. 在 workflow 中使用：
   ```yaml
   env:
     GITHUB_TOKEN: ${{ secrets.PAT_TOKEN }}
   ```

### 4. 检查仓库可见性

#### 私有仓库
- 确保你的账户有足够的权限
- 检查是否是组织仓库，需要组织权限

#### 公共仓库
- 通常不会有权限问题
- 检查是否有特殊限制

### 5. 测试权限

#### 创建测试分支
```bash
git checkout -b test-permissions
git push origin test-permissions
```

#### 检查 Actions 日志
1. 进入 Actions 标签页
2. 查看最新的工作流运行
3. 检查权限相关的错误信息

### 6. 常见权限错误及解决方案

#### 错误: "Resource not accessible by integration"
**解决方案**: 检查 Workflow permissions 设置

#### 错误: "Not Found"
**解决方案**: 检查仓库名称和分支名称是否正确

#### 错误: "Bad credentials"
**解决方案**: 检查 GITHUB_TOKEN 是否正确设置

### 7. 高级权限配置

#### 使用 Fine-grained permissions
```yaml
permissions:
  contents: write
  issues: write
  pull-requests: write
  actions: read
```

#### 使用最小权限原则
```yaml
permissions:
  contents: write  # 只允许写入内容（创建release）
  actions: read    # 只允许读取actions
```

### 8. 故障排除检查清单

- [ ] Workflow permissions 设置为 "Read and write permissions"
- [ ] 分支保护规则允许 Actions 写入
- [ ] GITHUB_TOKEN 正确配置
- [ ] 仓库可见性设置正确
- [ ] 账户有足够的权限
- [ ] 没有组织级别的限制

### 9. 联系支持

如果以上步骤都无法解决问题：
1. 检查 GitHub Status 页面
2. 在 GitHub Community 中搜索类似问题
3. 联系 GitHub Support

### 10. 安全注意事项

⚠️ **重要提醒**:
- 不要将 Personal Access Token 提交到代码中
- 定期轮换 PAT
- 使用最小必要权限
- 监控 Actions 的使用情况 