# Contributing to SecureTransfer

Thank you for your interest in contributing to SecureTransfer! This document provides guidelines and information for contributors.

## 🚀 Getting Started

### Prerequisites
- Java 21 LTS (OpenJDK or Oracle)
- Maven 3.8+ (for desktop development)
- Android SDK with Build Tools 34+ (for Android development)
- Git for version control

### Development Setup
1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/file-transfer-app.git
   cd file-transfer-app
   ```
3. Set up the development environment:
   ```bash
   # Install dependencies and build
   ./scripts/build-all.sh
   ```

## 🏗️ Project Structure

```
file-transfer-app/
├── desktop/                    # JavaFX desktop application
├── android/                   # Android application
├── server/                    # Signaling server
├── scripts/                   # Build and utility scripts
├── .github/workflows/         # CI/CD pipelines
└── docs/                      # Documentation
```

## 🔧 Development Workflow

### Making Changes
1. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Make your changes following the coding standards
3. Test your changes thoroughly:
   ```bash
   ./scripts/test-all.sh
   ```
4. Commit your changes with descriptive messages
5. Push to your fork and create a Pull Request

### Coding Standards

#### Java Code Style
- Use 4 spaces for indentation
- Follow Oracle Java naming conventions
- Maximum line length: 120 characters
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs

#### Commit Messages
- Use present tense ("Add feature" not "Added feature")
- Use imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit first line to 72 characters
- Reference issues and pull requests when applicable

Example:
```
Add drag-and-drop file transfer functionality

- Implement drag-and-drop handlers for desktop UI
- Add visual feedback during drag operations
- Update file transfer queue management
- Fixes #123
```

## 🧪 Testing

### Running Tests
```bash
# All tests
./scripts/test-all.sh

# Desktop tests only
cd desktop && mvn test

# Android tests only
cd android && ./gradlew test
```

### Test Coverage
- Aim for at least 80% code coverage
- Write unit tests for all new functionality
- Include integration tests for complex features
- Test edge cases and error conditions

## 📝 Documentation

### Code Documentation
- Add JavaDoc comments for all public classes and methods
- Include usage examples in documentation
- Document complex algorithms and business logic
- Keep README.md updated with new features

### User Documentation
- Update user guides for new features
- Include screenshots for UI changes
- Provide clear installation and setup instructions
- Document configuration options

## 🐛 Bug Reports

### Before Submitting
- Check existing issues to avoid duplicates
- Test with the latest version
- Gather relevant system information

### Bug Report Template
```markdown
**Describe the bug**
A clear description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '....'
3. See error

**Expected behavior**
What you expected to happen.

**Screenshots**
If applicable, add screenshots.

**Environment:**
- OS: [e.g. Windows 11, Ubuntu 22.04]
- Java Version: [e.g. OpenJDK 21]
- App Version: [e.g. 1.0.0]
```

## 💡 Feature Requests

### Before Submitting
- Check if the feature already exists
- Consider if it fits the project's scope
- Think about implementation complexity

### Feature Request Template
```markdown
**Is your feature request related to a problem?**
A clear description of what the problem is.

**Describe the solution you'd like**
A clear description of what you want to happen.

**Describe alternatives you've considered**
Other solutions you've considered.

**Additional context**
Any other context or screenshots.
```

## 🔍 Code Review Process

### For Contributors
- Ensure all tests pass
- Update documentation as needed
- Respond to review feedback promptly
- Keep pull requests focused and atomic

### For Reviewers
- Be constructive and respectful
- Focus on code quality and maintainability
- Check for security implications
- Verify test coverage

## 🏷️ Release Process

### Version Numbering
We use Semantic Versioning (SemVer):
- MAJOR.MINOR.PATCH (e.g., 1.2.3)
- MAJOR: Breaking changes
- MINOR: New features (backward compatible)
- PATCH: Bug fixes (backward compatible)

### Release Checklist
- [ ] All tests pass
- [ ] Documentation updated
- [ ] Version numbers bumped
- [ ] Release notes prepared
- [ ] Installers built and tested

## 🤝 Community Guidelines

### Code of Conduct
- Be respectful and inclusive
- Welcome newcomers and help them learn
- Focus on constructive feedback
- Respect different opinions and approaches

### Communication
- Use GitHub Issues for bug reports and feature requests
- Use GitHub Discussions for general questions
- Be patient and helpful with responses
- Search existing discussions before posting

## 📚 Resources

### Learning Resources
- [JavaFX Documentation](https://openjfx.io/javadoc/21/)
- [Android Developer Guide](https://developer.android.com/guide)
- [Maven Documentation](https://maven.apache.org/guides/)
- [Gradle User Manual](https://docs.gradle.org/current/userguide/userguide.html)

### Development Tools
- **IDEs**: IntelliJ IDEA, Eclipse, Android Studio
- **Build Tools**: Maven, Gradle
- **Testing**: JUnit 5, Mockito
- **CI/CD**: GitHub Actions

## 🙏 Recognition

Contributors will be recognized in:
- README.md contributors section
- Release notes
- GitHub contributors page

Thank you for contributing to SecureTransfer! 🚀
