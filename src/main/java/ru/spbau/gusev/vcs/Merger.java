package ru.spbau.gusev.vcs;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A module for merging commits.
 */
public abstract class Merger {
    /**
     * Merges the supplied branch into current.
     * @param branchToMerge the branch to merge into current.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     * @throws VCS.BadPositionException if the current position is not a head of a
     * branch.
     */
    @Nonnull
    protected static Commit merge(@Nonnull Branch branchToMerge) throws
            VCS.BadRepoException, VCS.BadPositionException {
        Commit curCommit = Repository.getCurrentCommit();
        Commit commitToMerge = branchToMerge.getHead();

        List<Commit> curCommitPedigree = curCommit.getPedigree();
        List<Commit> commitToMergePedigree = commitToMerge.getPedigree();

        int pos = 0;
        while (pos < curCommitPedigree.size() && pos < commitToMergePedigree.size() &&
                curCommitPedigree.get(pos).equals(commitToMergePedigree.get(pos))) {
            pos++;
        }
        Commit commonPredecessor = curCommitPedigree.get(pos - 1);

        Map<Path, TrackedFile> sourceFiles = commonPredecessor.getFiles()
                .collect(Collectors.toMap(TrackedFile::getName, file -> file));
        Map<Path, TrackedFile> mergedFiles = commitToMerge.getFiles()
                .collect(Collectors.toMap(TrackedFile::getName, file -> file));
        Map<Path, TrackedFile> curFiles = curCommit.getFiles()
                .collect(Collectors.toMap(TrackedFile::getName, file -> file));
        Map<Path, TrackedFile> resFiles = new HashMap<>();

        resFiles.putAll(sourceFiles);
        curFiles.forEach((path, hashedFile) -> {
            if (!sourceFiles.containsKey(path) || !hashedFile.equals(sourceFiles.get(path))) {
                resFiles.put(path, hashedFile);
            }
        });

        mergedFiles.forEach((path, hashedFile) -> {
            if (!sourceFiles.containsKey(path) || !hashedFile.equals(sourceFiles.get(path))) {
                resFiles.put(path, hashedFile);
            }
        });

        sourceFiles.forEach((name, desc) -> {
            if (!curFiles.containsKey(name) || !mergedFiles.containsKey(name)) {
                resFiles.remove(name);
            }
        });

        StagingZone stagingZone = Repository.getStagingZone();
        stagingZone.wipe();
        resFiles.forEach((path, desc) -> stagingZone.add(desc));

        Commit mergedCommit = Commit.create("Branch " + branchToMerge.getName() + " merged.");
        WorkingDirectory workingDirectory = Repository.getWorkingDirectory();
        curCommit.removeFrom(workingDirectory);
        mergedCommit.checkout(workingDirectory, stagingZone);
        Repository.setCurrentCommit(mergedCommit);
        return mergedCommit;
    }
}
