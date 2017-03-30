import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The module for merging commits.
 */
public abstract class Merger {
    /**
     * Merges the supplied branch into current.
     * @param branchToMerge the branch to merge into current.
     * @throws VCS.BadRepoException if the repository folder is corrupt.
     * @throws VCS.BadPositionException if the current position is not a head of a
     * branch.
     */
    protected static void merge(Branch branchToMerge) throws VCS.BadRepoException,
            VCS.BadPositionException{
        Commit curCommit = Repository.getCurrentCommit();
        Commit commitToMerge = branchToMerge.getHead();

        if (!curCommit.getBranch().getHead().equals(curCommit)) {
            throw new VCS.BadPositionException();
        }

        List<Commit> curCommitPedigree = curCommit.getPedigree();
        List<Commit> commitToMergePedigree = commitToMerge.getPedigree();

        int pos = 0;
        while (pos < curCommitPedigree.size() && pos < commitToMergePedigree.size() &&
                curCommitPedigree.get(pos).equals(commitToMergePedigree.get(pos))) {
            pos++;
        }
        Commit commonPredecessor = curCommitPedigree.get(pos - 1);

        Map<Path, HashedFile> sourceFiles = commonPredecessor.getFileDescriptions();
        Map<Path, HashedFile> mergedFiles = commitToMerge.getFileDescriptions();
        Map<Path, HashedFile> curFiles = curCommit.getFileDescriptions();
        Map<Path, HashedFile> resFiles = new HashMap<>();

        resFiles.putAll(curFiles);
        resFiles.putAll(mergedFiles);

        sourceFiles.forEach((name, desc) -> {
            if (!curFiles.containsKey(name) || !mergedFiles.containsKey(name)) {
                resFiles.remove(name);
            }
        });

        HashedDirectory stageDir = StagingZone.getDir();
        resFiles.forEach((path, desc) -> {
            try {
                Path realPath = desc.getPath();
                Path dirPath = realPath.subpath(0, realPath.getNameCount() -
                        path.getNameCount());
                stageDir.addFile(dirPath, path);
            } catch (VCS.NoSuchFileException e) {
                throw new VCS.FileSystemError();
            }
        });
        new Commit("Branch " + branchToMerge.getName() + " merged.");
    }
}
