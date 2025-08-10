# CI/CD 故障排除指南

## 常见问题及解决方案

### 1. APK文件路径问题

#### 问题描述
```
Pattern 'app/build/outputs/apk/**/*.apk' does not match any files.
```

#### 解决方案
1. **检查APK文件位置**：
   ```bash
   find . -name "*.apk" -type f
   ```

2. **修正workflow中的路径**：
   ```yaml
   path: |
     app/build/outputs/apk/debug/*.apk
     app/build/outputs/apk/release/*.apk
   ```

3. **确保构建成功**：
   - 检查build-android job是否成功
   - 验证APK文件是否生成

### 2. GitHub Release权限问题

#### 问题描述
```
GitHub release failed with status: 403
```

#### 解决方案
1. **检查仓库权限设置**：
   - 进入 GitHub 仓库 → Settings → Actions → General
   - 设置 "Workflow permissions" 为 "Read and write permissions"

2. **验证GITHUB_TOKEN**：
   - 确保使用 `${{ secrets.GITHUB_TOKEN }}`
   - 或者创建 Personal Access Token (PAT)

3. **检查分支保护规则**：
   - 确保main分支允许Actions写入
   - 检查是否有分支保护限制

### 3. Gradle配置问题

#### 问题描述
```
The option 'android.enableBuildCache' is deprecated.
```

#### 解决方案
1. **移除已弃用的配置**：
   - 从 `gradle.properties` 中移除 `android.enableBuildCache=true`
   - 从 `gradle-ci.properties` 中移除相同配置

2. **使用兼容的配置**：
   ```properties
   # 启用Gradle构建缓存（替代Android特定缓存）
   org.gradle.caching=true
   org.gradle.configuration-cache=true
   ```

### 4. 构建失败问题

#### 问题描述
```
Execution failed for task ':app:lintVitalRelease'
```

#### 解决方案
1. **修复Lint错误**：
   - 检查布局文件中的ID引用
   - 确保ConstraintLayout约束正确

2. **本地测试**：
   ```bash
   ./gradlew assembleRelease --no-daemon
   ```

3. **清理项目**：
   ```bash
   ./gradlew clean
   ```

### 5. 缓存问题

#### 问题描述
```
Gradle缓存阶段很慢，需要5分钟以上
```

#### 解决方案
1. **优化缓存策略**：
   - 使用分层缓存
   - 分别缓存依赖和构建输出

2. **检查缓存命中率**：
   - 查看Actions日志中的缓存信息
   - 确保缓存键设置正确

3. **清理无效缓存**：
   - 删除 `.gradle` 和 `build` 目录
   - 重新运行构建

## 调试步骤

### 1. 添加调试信息
在workflow中添加调试步骤：
```yaml
- name: 调试信息
  run: |
    echo "当前目录: $(pwd)"
    echo "文件列表:"
    ls -la
    echo "APK文件:"
    find . -name "*.apk" -type f
```

### 2. 检查文件路径
确保文件路径正确：
```bash
# 检查APK文件
find . -name "*.apk" -type f

# 检查目录结构
tree app/build/outputs/apk/
```

### 3. 验证权限
检查GitHub权限设置：
```yaml
- name: 检查权限
  run: |
    echo "GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN != '' && '已设置' || '未设置' }}"
    echo "分支: ${{ github.ref_name }}"
```

## 预防措施

### 1. 本地测试
在推送代码前，本地测试构建：
```bash
./gradlew test --no-daemon
./gradlew assembleDebug --no-daemon
./gradlew assembleRelease --no-daemon
```

### 2. 定期更新依赖
保持依赖版本更新：
```bash
./gradlew dependencyUpdates
```

### 3. 监控构建时间
关注构建时间变化，及时发现问题。

## 获取帮助

如果问题仍然存在：
1. 检查GitHub Actions日志
2. 查看本地构建输出
3. 参考官方文档
4. 在GitHub Issues中搜索类似问题 