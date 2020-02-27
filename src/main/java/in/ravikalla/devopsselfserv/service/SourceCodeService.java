package in.ravikalla.devopsselfserv.service;

import java.io.File;
import java.io.IOException;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import in.ravikalla.devopsselfserv.util.CustomGlobalContext;
import in.ravikalla.devopsselfserv.util.OrgName;
import in.ravikalla.devopsselfserv.util.ProjectType;

@Service
public class SourceCodeService {
	Logger L = LoggerFactory.getLogger(SourceCodeService.class);

	@Autowired
	private Environment env;

	public Repository create(String strOrg, String strToken, String strRepoName, Boolean blnIsPrivate, String strCloneURL) throws IOException {
		GitHubClient client = new GitHubClient();
		client.setOAuth2Token(strToken);
		RepositoryService repositoryService = new RepositoryService(client); // TODO : Optimize this by creating a Spring bean

		Repository repository  = null;
		try {
			repository = createProject(strOrg, strRepoName, blnIsPrivate, strCloneURL, repositoryService);
		} catch (IOException e) {
			L.error("29 : SourceCodeService.create(...) : IOException e = {}", e);
			throw e;
		}
		return repository;
	}

	public Repository gitFork(ProjectType projectType, OrgName newOrgName, String strProjectName) throws IOException {
		L.debug("Start : SourceCodeService.gitFork(...) : strTechnology = {}, strNewOrg = {}, strProjectName = {}", projectType, newOrgName, strProjectName);
		String strTemplateOrg = env.getProperty("git.orgname");
		String strTemplateRepoName = env.getProperty("git.projectname." + projectType.toString());

		GitHubClient client = new GitHubClient();
		client.setOAuth2Token(CustomGlobalContext.getGitToken());
		RepositoryService repositoryService = new RepositoryService(client); // TODO : Optimize this by creating a Spring bean

		Repository repository  = null;
		try {
			repository = forkProject(strTemplateOrg, strTemplateRepoName, newOrgName, repositoryService);
		} catch (IOException e) {
			L.error("44 : SourceCodeService.gitFork(...) : IOException e = {}", e);
			throw e;
		}
		L.debug("End : SourceCodeService.gitFork(...) : strTechnology = {}, strNewOrg = {}, strProjectName = {}", projectType, newOrgName, strProjectName);
		return repository;
	}

	private Repository createProject(String strOrg, String strRepoName, Boolean blnIsPrivate, String strCloneURL, RepositoryService repositoryService) throws IOException {
		Repository repository = new Repository();
		repository.setName(strRepoName);
		repository.setPrivate(blnIsPrivate);
		repository.setCloneUrl(strCloneURL);
		Repository createRepository = repositoryService.createRepository(strOrg, repository);
		return createRepository;
	}

	private Repository forkProject(String strOrg, String strRepoName, OrgName newOrgName, RepositoryService repositoryService) throws IOException {
		RepositoryId repo = new RepositoryId(strOrg, strRepoName);
		Repository createRepository = repositoryService.forkRepository(repo, newOrgName.toString());
		return createRepository;
	}

	public void cloneRepo(String owner, String repoName, RepositoryService rs, String LOCAL_TEMP_PATH)
			throws Exception {
		Git result = null;
		try {
			Repository r = rs.getRepository(owner, repoName);

			String cloneURL = r.getSshUrl();
			// prepare a new folder for the cloned repository
			File localPath = new File(LOCAL_TEMP_PATH + File.separator + owner + File.separator + repoName);
			if (localPath.isDirectory() == false) {
				localPath.mkdirs();
			} else {
				throw new Exception("Local directory already exists. Delete it first: " + localPath);
			}

			L.debug("Cloning from " + cloneURL + " to " + localPath);
			result = Git.cloneRepository().setURI(cloneURL).setDirectory(localPath).call();
			// Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
			L.debug("Cloned repository: " + result.getRepository().getDirectory());
		} catch (IOException | GitAPIException ex) {
			throw new Exception("Problem cloning repo: " + ex.getMessage());
		} finally {
			if (result != null) {
				result.close();
			}
		}
	}
}
