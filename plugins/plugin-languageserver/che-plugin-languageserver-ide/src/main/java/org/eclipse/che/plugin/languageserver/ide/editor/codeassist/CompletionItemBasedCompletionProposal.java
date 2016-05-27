package org.eclipse.che.plugin.languageserver.ide.editor.codeassist;

import com.google.gwt.user.client.ui.Widget;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.api.editor.codeassist.Completion;
import org.eclipse.che.ide.api.editor.codeassist.CompletionProposal;
import org.eclipse.che.ide.api.editor.document.Document;
import org.eclipse.che.ide.api.editor.text.LinearRange;
import org.eclipse.che.ide.api.editor.text.TextPosition;
import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.plugin.languageserver.ide.service.TextDocumentServiceClient;
import org.eclipse.che.plugin.languageserver.shared.lsapi.CompletionItemDTO;
import org.eclipse.che.plugin.languageserver.shared.lsapi.RangeDTO;
import org.eclipse.che.plugin.languageserver.shared.lsapi.TextDocumentIdentifierDTO;

class CompletionItemBasedCompletionProposal implements CompletionProposal {

    private final TextDocumentServiceClient documentServiceClient;
    private final TextDocumentIdentifierDTO documentId;
    private       CompletionItemDTO         completionItem;

    CompletionItemBasedCompletionProposal(CompletionItemDTO completionItem,
                                          TextDocumentServiceClient documentServiceClient,
                                          TextDocumentIdentifierDTO documentId) {
        this.completionItem = completionItem;
        this.documentServiceClient = documentServiceClient;
        this.documentId = documentId;
    }

    @Override
    public Widget getAdditionalProposalInfo() {
        return null;
    }

    @Override
    public String getDisplayString() {
        return completionItem.getLabel();
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public void getCompletion(final CompletionCallback callback) {
        //call resolve only if we dont have TextEdit in CompletionItem
        //TODO we need to  check also CompletionItem#getInsertText();
        if (completionItem.getTextEdit() == null) {
            completionItem.setTextDocumentIdentifier(documentId);
            documentServiceClient.resolveCompletionItem(completionItem).then(new Operation<CompletionItemDTO>() {
                @Override
                public void apply(CompletionItemDTO arg) throws OperationException {
                    callback.onCompletion(new CompletionImpl(arg));
                }
            });
        } else {
            callback.onCompletion(new CompletionImpl(completionItem));
        }
    }

    private static class CompletionImpl implements Completion {

        private CompletionItemDTO completionItem;

        public CompletionImpl(CompletionItemDTO completionItem) {
            this.completionItem = completionItem;
        }

        @Override
        public void apply(Document document) {
            //TODO in general resolve completion item may not provide getTextEdit, need to add checks
            RangeDTO range = completionItem.getTextEdit().getRange();
            int startOffset = document.getIndexFromPosition(
                    new TextPosition(range.getStart().getLine(), range.getStart().getCharacter()));
            int endOffset = document
                    .getIndexFromPosition(new TextPosition(range.getEnd().getLine(), range.getEnd().getCharacter()));
            document.replace(startOffset, endOffset - startOffset, completionItem.getTextEdit().getNewText());
        }

        @Override
        public LinearRange getSelection(Document document) {
            RangeDTO range = completionItem.getTextEdit().getRange();
            int startOffset = document
                                      .getIndexFromPosition(new TextPosition(range.getStart().getLine(), range.getStart().getCharacter()))
                              + completionItem.getTextEdit().getNewText().length();
            return LinearRange.createWithStart(startOffset).andLength(0);
        }

    }

}